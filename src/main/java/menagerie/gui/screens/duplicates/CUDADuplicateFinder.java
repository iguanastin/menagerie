/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.gui.screens.duplicates;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.histogram.ImageHistogram;

import java.util.ArrayList;
import java.util.List;


public class CUDADuplicateFinder {

    /**
     * Path to kernel .ptx file
     */
    private static final String PTX = "./histdupe.ptx";
    public static final String KERNEL_FUNCTION_NAME = "_Z14histDupeKernelPKfS0_S0_S0_PiS1_S1_S1_PfS1_iii";


    /**
     * Uses a CUDA device to find similar pairs of items based on their histograms.
     *
     * @param smallSet   Set of items to compare
     * @param largeSet   Set of items to compare against
     * @param confidence Confidence for similarity, value between 0 and 1
     * @param maxResults Maximum number of results to return
     * @return Set of similar pairs found with the given confidence
     */
    public static List<SimilarPair<MediaItem>> findDuplicates(final List<Item> smallSet, final List<Item> largeSet, final float confidence, final int maxResults) {

        // Construct a clean dataset
        List<MediaItem> trueSet1 = getCleanedSet(smallSet);
        List<MediaItem> trueSet2 = getCleanedSet(largeSet);

        // Get adjusted dataset size. Padded to avoid memory access errors with 64 thread blocks.
        int N1 = (int) Math.ceil(trueSet1.size() / 64.0) * 64;
        int N2 = trueSet2.size();

        // Initialize the device and kernel
        CUfunction function = initCUFunction();

        // Init data array
        float[] data1 = initDataArray(trueSet1, N1);
        float[] data2 = initDataArray(trueSet2, N2);
        // Init confidence array
        float[] confs1 = initConfsArray(confidence, trueSet1, N1);
        float[] confs2 = initConfsArray(confidence, trueSet2, N2);
        //Init ids arrays
        int[] ids1 = initIdsArray(trueSet1, N1);
        int[] ids2 = initIdsArray(trueSet2, N2);

        // Allocate and copy data to device
        CUdeviceptr d_data1 = new CUdeviceptr();
        CUdeviceptr d_data2 = new CUdeviceptr();
        CUdeviceptr d_confs1 = new CUdeviceptr();
        CUdeviceptr d_confs2 = new CUdeviceptr();
        CUdeviceptr d_ids1 = new CUdeviceptr();
        CUdeviceptr d_ids2 = new CUdeviceptr();
        CUdeviceptr d_resultsID1 = new CUdeviceptr();
        CUdeviceptr d_resultsID2 = new CUdeviceptr();
        CUdeviceptr d_resultsSimilarity = new CUdeviceptr();
        CUdeviceptr d_resultCount = new CUdeviceptr();
        allocateAndCopyToDevice(maxResults, N1, N2, data1, data2, confs1, confs2, ids1, ids2, d_data1, d_data2, d_confs1, d_confs2, d_ids1, d_ids2, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        // Launch kernel
        launchKernel(maxResults, function, N1, N2, d_data1, d_data2, d_confs1, d_confs2, d_ids1, d_ids2, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        // Get results from device
        List<SimilarPair<MediaItem>> results = getResultsFromDevice(trueSet1, trueSet2, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        // Free device memory
        freeDeviceMemory(d_data1, d_data2, d_confs1, d_confs2, d_ids1, d_ids2, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        return results;
    }

    private static int[] initIdsArray(List<MediaItem> set, int n) {
        int[] ids1 = new int[n];
        for (int i = 0; i < n; i++) {
            if (i < set.size()) ids1[i] = set.get(i).getId();
        }
        return ids1;
    }

    private static void freeDeviceMemory(CUdeviceptr... pointers) {
        for (CUdeviceptr pointer : pointers) {
            JCudaDriver.cuMemFree(pointer);
        }
    }

    private static List<MediaItem> getCleanedSet(List<Item> set) {
        // Remove all items without histograms
        List<MediaItem> trueSet = new ArrayList<>();
        set.forEach(item -> {
            if (item instanceof MediaItem && ((MediaItem) item).getHistogram() != null) trueSet.add((MediaItem) item);
        });
        return trueSet;
    }

    private static CUfunction initCUFunction() {
        // Init exceptions
        JCudaDriver.setExceptionsEnabled(true);

        // Init device and context
        JCudaDriver.cuInit(0);
        CUdevice device = new CUdevice();
        JCudaDriver.cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        JCudaDriver.cuCtxCreate(context, 0, device);

        // Load CUDA module
        CUmodule module = new CUmodule();
        JCudaDriver.cuModuleLoad(module, PTX);

        // Get function reference
        CUfunction function = new CUfunction();
        JCudaDriver.cuModuleGetFunction(function, module, KERNEL_FUNCTION_NAME);
        return function;
    }

    private static List<SimilarPair<MediaItem>> getResultsFromDevice(List<MediaItem> set1, List<MediaItem> set2, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        // Get result count from device
        int[] resultCountArr = new int[1];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultCountArr), d_resultCount, Sizeof.INT);
        int resultCount = resultCountArr[0];
        // Get results from device
        int[] resultsID1 = new int[resultCount];
        int[] resultsID2 = new int[resultCount];
        float[] resultsSimilarity = new float[resultCount];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultsID1), d_resultsID1, resultCount * Sizeof.INT);
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultsID2), d_resultsID2, resultCount * Sizeof.INT);
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultsSimilarity), d_resultsSimilarity, resultCount * Sizeof.FLOAT);

        List<SimilarPair<MediaItem>> results = new ArrayList<>();
        for (int i = 0; i < resultCount; i++) {
            SimilarPair<MediaItem> pair = new SimilarPair<>(getItemByID(resultsID1[i], set1, set2), getItemByID(resultsID2[i], set1, set2), resultsSimilarity[i]);

            if (!results.contains(pair)) results.add(pair);
        }

        return results;
    }

    private static MediaItem getItemByID(int id, List<MediaItem> set1, List<MediaItem> set2) {
        for (MediaItem item : set1) {
            if (item.getId() == id) return item;
        }
        for (MediaItem item : set2) {
            if (item.getId() == id) return item;
        }

        return null;
    }

    private static void launchKernel(int maxResults, CUfunction function, int N1, int N2, CUdeviceptr d_data1, CUdeviceptr d_data2, CUdeviceptr d_confs1, CUdeviceptr d_confs2, CUdeviceptr d_ids1, CUdeviceptr d_ids2, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        // Set up kernel parameters
        Pointer kernelParameters = Pointer.to(
                Pointer.to(d_data1),
                Pointer.to(d_data2),
                Pointer.to(d_confs1),
                Pointer.to(d_confs2),
                Pointer.to(d_ids1),
                Pointer.to(d_ids2),
                Pointer.to(d_resultsID1),
                Pointer.to(d_resultsID2),
                Pointer.to(d_resultsSimilarity),
                Pointer.to(d_resultCount),
                Pointer.to(new int[]{N1}),
                Pointer.to(new int[]{N2}),
                Pointer.to(new int[]{maxResults})
        );

        // Launch kernel
        JCudaDriver.cuLaunchKernel(function, (int) Math.ceil(N1 / 64.0), 1, 1, 64, 1, 1, 0, null, kernelParameters, null);
        JCudaDriver.cuCtxSynchronize();
    }

    private static void allocateAndCopyToDevice(int maxResults, int N1, int N2, float[] data1, float[] data2, float[] confs1, float[] confs2, int[] ids1, int[] ids2, CUdeviceptr d_data1, CUdeviceptr d_data2, CUdeviceptr d_confs1, CUdeviceptr d_confs2, CUdeviceptr d_ids1, CUdeviceptr d_ids2, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        long bytes = N1 * ImageHistogram.BIN_SIZE * ImageHistogram.NUM_CHANNELS * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_data1, bytes);
        JCudaDriver.cuMemcpyHtoD(d_data1, Pointer.to(data1), bytes);
        bytes = N2 * ImageHistogram.BIN_SIZE * ImageHistogram.NUM_CHANNELS * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_data2, bytes);
        JCudaDriver.cuMemcpyHtoD(d_data2, Pointer.to(data2), bytes);

        // Allocate and copy confs to device
        bytes = N1 * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_confs1, bytes);
        JCudaDriver.cuMemcpyHtoD(d_confs1, Pointer.to(confs1), bytes);
        bytes = N2 * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_confs2, bytes);
        JCudaDriver.cuMemcpyHtoD(d_confs2, Pointer.to(confs2), bytes);

        // Allocate and copy ids to device
        bytes = N1 * Sizeof.INT;
        JCudaDriver.cuMemAlloc(d_ids1, bytes);
        JCudaDriver.cuMemcpyHtoD(d_ids1, Pointer.to(ids1), bytes);
        bytes = N2 * Sizeof.INT;
        JCudaDriver.cuMemAlloc(d_ids2, bytes);
        JCudaDriver.cuMemcpyHtoD(d_ids2, Pointer.to(ids2), bytes);

        // Allocate results arrays on device
        bytes = maxResults * Sizeof.INT;
        JCudaDriver.cuMemAlloc(d_resultsID1, bytes);
        JCudaDriver.cuMemAlloc(d_resultsID2, bytes);
        bytes = maxResults * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_resultsSimilarity, bytes);

        // Allocate result count on device
        JCudaDriver.cuMemAlloc(d_resultCount, Sizeof.INT);
    }

    private static float[] initConfsArray(float confidence, List<MediaItem> trueSet, int N) {
        float[] confs = new float[N];
        final float confidenceSquare = 1 - (1 - confidence) * (1 - confidence);
        for (int i = 0; i < N; i++) {
            if (i < trueSet.size()) {
                confs[i] = trueSet.get(i).getHistogram().isColorful() ? confidence : confidenceSquare;
            } else {
                confs[i] = 2; // Impossible confidence
            }
        }
        return confs;
    }

    private static float[] initDataArray(List<MediaItem> trueSet, int N) {
        final int size = ImageHistogram.BIN_SIZE * ImageHistogram.NUM_CHANNELS;
        float[] data = new float[N * size];
        for (int i = 0; i < trueSet.size(); i++) {
            final ImageHistogram hist = trueSet.get(i).getHistogram();
            for (int j = 0; j < ImageHistogram.BIN_SIZE; j++) {
                // Convert to float because GPUs work best with single precision
                data[i * size + j] = (float) hist.getAlpha()[j];
                data[i * size + j + ImageHistogram.BIN_SIZE] = (float) hist.getRed()[j];
                data[i * size + j + ImageHistogram.BIN_SIZE * 2] = (float) hist.getGreen()[j];
                data[i * size + j + ImageHistogram.BIN_SIZE * 3] = (float) hist.getBlue()[j];
            }
        }
        return data;
    }

}
