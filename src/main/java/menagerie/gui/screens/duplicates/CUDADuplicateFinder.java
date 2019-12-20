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


    /**
     * Uses a CUDA device to find similar pairs of items based on their histograms.
     *
     * @param set        Set of items to compare
     * @param confidence Confidence for similarity, value between 0 and 1
     * @param maxResults Maximum number of results to return
     * @return Set of similar pairs found with the given confidence
     */
    public static List<SimilarPair<Item>> findDuplicates(final List<Item> set, final float confidence, final int maxResults) {

        // Construct a clean dataset
        List<MediaItem> trueSet = getCleanedSet(set);

        // Get adjusted dataset size. Padded to avoid memory access errors with 64 thread blocks.
        int N = (int) Math.ceil(trueSet.size() / 64.0) * 64;

        // Initialize the device and kernel
        CUfunction function = initCUFunction();

        // Init data array
        float[] data = initDataArray(trueSet, N);
        // Init confidence array
        float[] confs = initConfsArray(confidence, trueSet, N);

        // Allocate and copy data to device
        CUdeviceptr d_data = new CUdeviceptr();
        CUdeviceptr d_confs = new CUdeviceptr();
        CUdeviceptr d_resultsID1 = new CUdeviceptr();
        CUdeviceptr d_resultsID2 = new CUdeviceptr();
        CUdeviceptr d_resultsSimilarity = new CUdeviceptr();
        CUdeviceptr d_resultCount = new CUdeviceptr();
        allocateAndCopyToDevice(maxResults, N, data, confs, d_data, d_confs, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        // Launch kernel
        launchKernel(maxResults, trueSet, function, N, d_data, d_confs, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        // Get results from device
        List<SimilarPair<Item>> results = getResultsFromDevice(trueSet, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        // Free device memory
        freeDeviceMemory(d_data, d_confs, d_resultsID1, d_resultsID2, d_resultsSimilarity, d_resultCount);

        return results;
    }

    private static void freeDeviceMemory(CUdeviceptr d_data, CUdeviceptr d_confs, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        JCudaDriver.cuMemFree(d_data);
        JCudaDriver.cuMemFree(d_confs);
        JCudaDriver.cuMemFree(d_resultsID1);
        JCudaDriver.cuMemFree(d_resultsID2);
        JCudaDriver.cuMemFree(d_resultsSimilarity);
        JCudaDriver.cuMemFree(d_resultCount);
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
        JCudaDriver.cuModuleGetFunction(function, module, "histDupeKernel");
        return function;
    }

    private static List<SimilarPair<Item>> getResultsFromDevice(List<MediaItem> set, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        // Get result count from device
        int[] resultCountArr = new int[1];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultCountArr), d_resultCount, Sizeof.INT);
        int resultCount = resultCountArr[0];
        // Get results from device
        int[] resultsID1 = new int[resultCount];
        int[] resultsID2 = new int[resultCount];
        int[] resultsSimilarity = new int[resultCount];
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultsID1), d_resultsID1, resultCount * Sizeof.INT);
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultsID2), d_resultsID2, resultCount * Sizeof.INT);
        JCudaDriver.cuMemcpyDtoH(Pointer.to(resultsSimilarity), d_resultsSimilarity, resultCount * Sizeof.FLOAT);

        List<SimilarPair<Item>> results = new ArrayList<>();
        for (int i = 0; i < resultCount; i++) {
            SimilarPair<Item> pair = new SimilarPair<>(set.get(resultsID1[i]), set.get(resultsID2[i]), resultsSimilarity[i]);

            if (!results.contains(pair)) results.add(pair);
        }

        return results;
    }

    private static void launchKernel(int maxResults, List<MediaItem> trueSet, CUfunction function, int n, CUdeviceptr d_data, CUdeviceptr d_confs, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        // Set up kernel parameters
        Pointer kernelParameters = Pointer.to(Pointer.to(d_data), Pointer.to(d_confs), Pointer.to(d_resultsID1), Pointer.to(d_resultsID2), Pointer.to(d_resultsSimilarity), Pointer.to(d_resultCount), Pointer.to(new int[]{n}), Pointer.to(new int[]{maxResults}));

        // Launch kernel
        JCudaDriver.cuLaunchKernel(function, (int) Math.ceil(trueSet.size() / 64.0), 1, 1, 64, 1, 1, 0, null, kernelParameters, null);
        JCudaDriver.cuCtxSynchronize();
    }

    private static void allocateAndCopyToDevice(int maxResults, int n, float[] data, float[] confs, CUdeviceptr d_data, CUdeviceptr d_confs, CUdeviceptr d_resultsID1, CUdeviceptr d_resultsID2, CUdeviceptr d_resultsSimilarity, CUdeviceptr d_resultCount) {
        long bytes = n * ImageHistogram.BIN_SIZE * ImageHistogram.NUM_CHANNELS * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_data, bytes);
        JCudaDriver.cuMemcpyHtoD(d_data, Pointer.to(data), bytes);
        // Allocate and copy confs to device
        bytes = n * Sizeof.FLOAT;
        JCudaDriver.cuMemAlloc(d_confs, bytes);
        JCudaDriver.cuMemcpyHtoD(d_confs, Pointer.to(confs), bytes);
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
