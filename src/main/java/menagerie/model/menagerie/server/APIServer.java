/*
 MIT License

 Copyright (c) 2020. Austin Thompson

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

package menagerie.model.menagerie.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import menagerie.gui.Thumbnail;
import menagerie.model.menagerie.*;
import menagerie.model.search.Search;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class APIServer {

    /**
     * HTTP server
     */
    private HttpServer server;

    /**
     * Menagerie to retrieve data from
     */
    private final Menagerie menagerie;

    /**
     * Number of items to return per page of search
     */
    private int pageSize;


    /**
     * Constructs a Menagerie API server
     *
     * @param menagerie The Menagerie to pull data from for requests
     * @param pageSize  Number of items to return per page
     */
    public APIServer(Menagerie menagerie, int pageSize) {
        this.menagerie = menagerie;
        this.pageSize = pageSize;
    }

    /**
     * Starts the API server on the specific port
     *
     * @param port Port of the server
     * @throws IOException When server fails to start
     */
    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        //        server = HttpsServer.create(new InetSocketAddress(54321), 0);
        //        SSLContext context = SSLContext.getInstance("TLS");
        //
        //        // Initialise the keystore
        //        char[] password = "simulator".toCharArray();
        //        KeyStore ks = KeyStore.getInstance("JKS");
        //        ks.load(getClass().getResourceAsStream("/lig.keystore"), password);
        //
        //        // Set up the key manager factory
        //        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        //        kmf.init(ks, password);
        //
        //        // Set up the trust manager factory
        //        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        //        tmf.init(ks);
        //
        //        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        //        server.setHttpsConfigurator(new HttpsConfigurator(context) {
        //            public void configure(HttpsParameters params) {
        //                try {
        //                    // Initialise the SSL context
        //                    SSLContext c = SSLContext.getDefault();
        //                    SSLEngine engine = c.createSSLEngine();
        //                    params.setNeedClientAuth(false);
        //                    params.setCipherSuites(engine.getEnabledCipherSuites());
        //                    params.setProtocols(engine.getEnabledProtocols());
        //
        //                    // Get the default parameters
        //                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
        //                    params.setSSLParameters(defaultSSLParameters);
        //                } catch (Exception ex) {
        //                    ex.printStackTrace();
        //                }
        //            }
        //        });

        server.createContext("/").setHandler(this::handleRequest);
        server.start();
    }

    /**
     * Stops the server if it is alive
     */
    public void stop() {
        if (server != null) server.stop(0);
        server = null;
    }

    /**
     * Sets the size of pages this server should respond with
     *
     * @param pageSize New size of pages
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     *
     * @return The size of page this server responds with
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Attempts to route a server request. Sends an error response if the request cannot be routed to a handler.
     *
     * @param exchange Exchange data of the request
     * @throws IOException When an IO exception occurs during the exchange
     */
    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            String target = exchange.getRequestURI().getPath().substring(1).toLowerCase();
            System.out.println(exchange.getRemoteAddress() + " requested: \"" + exchange.getRequestURI() + "\"");

            if (target.startsWith("thumbs/")) {
                handleThumbnailRequest(exchange);
            }

            switch (target) {
                case "hello":
                    sendSimpleResponse(exchange, 200, "Hi there!");
                    break;
                case "search":
                    handleSearchRequest(exchange);
                    break;
                case "tags":
                    handleTagsRequest(exchange);
                    break;
                default:
                    sendSimpleResponse(exchange, 404, makeErrorHTML(exchange, 404, "No such endpoint", "No endpoint found at specified path"));
                    break;
            }
        } catch (Exception e) {
            sendSimpleResponse(exchange, 500, makeErrorHTML(exchange, 500, "Unexpected error", "Unexpected internal server error"));
            e.printStackTrace();
        }
    }

    /**
     * Handles requests for the tags endpoint.
     *
     * @param exchange The exchange
     * @throws IOException When an IO exception occurs during the exchange
     */
    private void handleTagsRequest(HttpExchange exchange) throws IOException {
        Map<String, String> query = mapQuerys(exchange);

        List<Tag> tags = new ArrayList<>(menagerie.getTags());

        if (query.containsKey("id")) {
            final int id = Integer.parseInt(query.get("id"));
            tags.removeIf(tag -> tag.getId() != id);
        }
        if (query.containsKey("name")) {
            final String name = query.get("name");
            tags.removeIf(tag -> !tag.getName().equalsIgnoreCase(name));
        }
        if (query.containsKey("starts")) {
            final String starts = query.get("starts").toLowerCase();
            tags.removeIf(tag -> !tag.getName().startsWith(starts));
        }
        if (query.containsKey("color")) {
            final String color = query.get("color").toLowerCase();
            tags.removeIf(tag -> !color.equalsIgnoreCase(tag.getColor()));
        }

        JSONObject json = new JSONObject();
        tags.forEach(tag -> {
            JSONObject j = new JSONObject();

            j.put("id", tag.getId());
            j.put("name", tag.getName());
            j.put("color", tag.getColor());
            j.put("notes", tag.getNotes());
            j.put("frequency", tag.getFrequency());

            json.append("tags", j);
        });

        exchange.setAttribute("Content-Type", "application/json");
        sendSimpleResponse(exchange, 200, json.toString());
    }

    /**
     * Handles requests for the search endpoint
     *
     * @param exchange The exchange
     * @throws IOException When an IO exception occurs during the exchange
     */
    private void handleSearchRequest(HttpExchange exchange) throws IOException {
        Map<String, String> query = mapQuerys(exchange);
        String terms = query.getOrDefault("terms", "");

        final int page = Integer.parseInt(query.getOrDefault("page", "0"));
        final boolean descending = "1".equalsIgnoreCase(query.get("desc"));
        final boolean ungrouped = "1".equalsIgnoreCase(query.get("ungroup"));
        final boolean expandTags = "1".equalsIgnoreCase(query.get("expand_tags"));
        final boolean expandGroups = "1".equalsIgnoreCase(query.get("expand_groups"));

        Search search = new Search(terms, descending, ungrouped, false, -1);
        search.refreshSearch(menagerie.getItems());

        final int total = search.getResults().size();
        final int count = Integer.min(pageSize, total - page * pageSize);

        JSONObject json = new JSONObject();
        json.put("page", page).put("count", count).put("total", total);

        for (int i = page * pageSize; i < page * pageSize + count; i++) {
            json.append("items", encodeJSONItem(search.getResults().get(i), expandTags, expandGroups));
        }

        exchange.setAttribute("Content-Type", "application/json");
        sendSimpleResponse(exchange, 200, json.toString());
    }

    /**
     * Handles requests for the thumbnail endpoint
     *
     * @param exchange The exchange
     * @throws IOException When an IO exception occurs during the exchange
     */
    private void handleThumbnailRequest(HttpExchange exchange) throws IOException {
        String idStr = exchange.getRequestURI().getPath().substring(8);
        idStr = idStr.substring(0, idStr.indexOf(".jpg"));
        try {
            int id = Integer.parseInt(idStr);

            if (menagerie != null) {
                Item item = menagerie.getItemByID(id);

                if (item != null) {
                    Thumbnail thumb = item.getThumbnail();
                    thumb.want();

                    if (!thumb.isLoaded()) {
                        // Await thumbnail
                        final CountDownLatch cdl = new CountDownLatch(1);
                        thumb.addImageReadyListener(thing -> cdl.countDown());
                        cdl.await();
                    }

                    sendImageResponse(exchange, thumb.getImage());
                    thumb.doNotWant();
                } else {
                    sendSimpleResponse(exchange, 404, makeErrorHTML(exchange, 404, "404 not found", "No such item with id: " + id));
                }
            } else {
                sendSimpleResponse(exchange, 500, makeErrorHTML(exchange, 500, "Not configured", "Server not connected to Menagerie"));
            }
        } catch (NumberFormatException e) {
            sendSimpleResponse(exchange, 400, makeErrorHTML(exchange, 400, "Invalid query", "Invalid id format: " + idStr));
        } catch (InterruptedException e) {
            sendSimpleResponse(exchange, 500, makeErrorHTML(exchange, 500, "Thumbnail error", "Failed to create/load thumbnail"));
        }
    }

    /**
     * Sends an image as a response for an exchange
     *
     * @param exchange The exchange
     * @param jfxImage Image to send to the client
     * @throws IOException When an IO exception occurs during the exchange
     */
    private void sendImageResponse(HttpExchange exchange, Image jfxImage) throws IOException {
        try {
            BufferedImage bImage = SwingFXUtils.fromFXImage(jfxImage, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "jpg", baos);
            baos.close();

            exchange.sendResponseHeaders(200, baos.size());
            OutputStream os = exchange.getResponseBody();
            exchange.setAttribute("Content-Type", "image/jpeg");
            os.write(baos.toByteArray());
            os.close();
        } catch (Exception e) {
            sendSimpleResponse(exchange, 500, makeErrorHTML(exchange, 500, "Transfer error", "Unexpected error sending image"));
        }
    }

    /**
     * Sends a simple text response to the client
     *
     * @param exchange Exchange
     * @param httpCode HTTP response code
     * @param response Text content of the response
     * @throws IOException When an IO exception occurs during the exchange
     */
    private static void sendSimpleResponse(HttpExchange exchange, int httpCode, String response) throws IOException {
        exchange.sendResponseHeaders(httpCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * Encodes the metadata of a Menagerie item as JSON for a search response
     *
     * @param item         Menagerie item to encode
     * @param expandTags   Expand tag information for this item
     * @param expandGroups Expand group elements for this item
     * @return A JSON representation of the item metadata
     */
    private JSONObject encodeJSONItem(Item item, boolean expandTags, boolean expandGroups) {
        JSONObject json = new JSONObject();

        String type = "unknown";
        if (item instanceof MediaItem) type = "media";
        else if (item instanceof GroupItem) type = "group";

        json.put("id", item.getId());
        json.put("thumbnail", "/thumbs/" + item.getId() + ".jpg");
        json.put("type", type);
        json.put("added", item.getDateAdded());
        item.getTags().forEach(tag -> {
            if (expandTags) {
                JSONObject j = new JSONObject();

                j.put("id", tag.getId());
                j.put("name", tag.getName());
                j.put("color", tag.getColor());
                j.put("notes", tag.getNotes());
                j.put("frequency", tag.getFrequency());

                json.append("tags", j);
            } else {
                json.append("tags", tag.getId());
            }
        });

        if (item instanceof MediaItem) {
            json.put("md5", ((MediaItem) item).getMD5());
            json.put("file", ((MediaItem) item).getFile().getAbsolutePath());
            if (((MediaItem) item).getGroup() != null) {
                json.put("element_of", ((MediaItem) item).getGroup().getId());
                json.put("element_index", ((MediaItem) item).getPageIndex());
            }
        } else if (item instanceof GroupItem) {
            ((GroupItem) item).getElements().forEach(element -> {
                if (expandGroups) {
                    json.append("elements", encodeJSONItem(element, expandTags, false));
                } else {
                    json.append("elements", element.getId());
                }
            });
            json.put("title", ((GroupItem) item).getTitle());
        }

        return json;
    }

    /**
     * Cleanly maps a query from a client into a map. A valueless parameter will be given a value of "1"
     *
     * @param exchange Exchange
     * @return A map of parameters and values
     */
    private Map<String, String> mapQuerys(HttpExchange exchange) {
        Map<String, String> query = new HashMap<>();

        String queryStr = exchange.getRequestURI().getRawQuery();
        if (queryStr != null) {
            String[] split = queryStr.split("&");
            for (String s : split) {
                try {
                    s = URLDecoder.decode(s, StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (s != null && !s.isEmpty()) {
                    if (s.contains("=")) {
                        query.put(s.substring(0, s.indexOf('=')), s.substring(s.indexOf('=') + 1));
                    } else {
                        query.put(s, "1");
                    }
                }
            }
        }
        return query;
    }

    /**
     * Constructs a pretty HTML error page with the given information
     *
     * @param exchange Exchange
     * @param code     HTTP Response code
     * @param title    Title of the page
     * @param message  Message
     * @return Text form of a pretty HTML error page
     */
    private static String makeErrorHTML(HttpExchange exchange, int code, String title, String message) {
        return "<!DOCTYPE html><html><head><title>" + code + ": " + title + "</title></head><body style=\"text-align: center; margin: 5em; line-height: 1.5em;\"><h1>Response Code " + code + "</h1><h2>" + title + "</h2><p>" + message + "<br>Endpoint: " + exchange.getRequestURI().getPath() + "<br>Query: " + exchange.getRequestURI().getQuery() + "<br>" + new Date() + "</p></body></html>";
    }


    public static void main(String[] args) throws IOException {
        APIServer server = new APIServer(null, 100);

        server.start(54321);
    }

}
