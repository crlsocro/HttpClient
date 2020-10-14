package no.kristiania.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HttpServer {

    private File contentRoot;
    private ProductDao productDao;
    //private List<String> projectNames = new ArrayList<>();
    //private List<String> projectEmail = new ArrayList<>();

    public HttpServer(int port, DataSource dataSource) throws IOException {
        productDao = new ProductDao(dataSource);
        ServerSocket serverSocket = new ServerSocket(port);

        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        HttpMessage request = new HttpMessage(clientSocket);
        String requestLine = request.getStartLine();
        System.out.println(requestLine);

        String requestMethod = requestLine.split(" ")[0];
        String requestTarget = requestLine.split(" ")[1];
        //String statusCode = "200";
        String body = "<a href=\"index.html\">Return to front page</a>";

        int questionPos = requestTarget.indexOf('?');
        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (requestMethod.equals("POST")) {
            QueryString requestParameter = new QueryString(request.getBody());
            productDao.insert(requestParameter.getParameter("productName"));
            //projectNames.add(requestParameter.getParameter("name"));
            //projectEmail.add(requestParameter.getParameter("email"));
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "\r\n" +
                    body;
            clientSocket.getOutputStream().write(response.getBytes());
        } else {//(requestPath.equals("/members.html")){
            if (requestPath.equals("/echo")) {
                handleEchoRequest(clientSocket, requestTarget, questionPos);
            } else if (requestPath.equals("/members.html")) {
                handleGetMembers(clientSocket);
            }
        }else{
            File file = new File(contentRoot, requestPath);
        }
        if (!file.exists()) {
            String body = file + " does not exist";
            String response = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "\r\n" +
                    body;
            // Write the response back to the client
            clientSocket.getOutputStream().write(response.getBytes());
            return;
        }
        String statusCode = "200";
        String contentType = "text/plain";
        if (file.getName().endsWith(".html")) {
            contentType = "text/html";
        }
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + file.length() + "\r\n" +
                "Connection: close\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "\r\n";
        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());

        new FileInputStream(file).transferTo(clientSocket.getOutputStream());
    }



    //handleGetMembers(clientSocket);}

        /*if (questionPos != -1) {
            QueryString queryString = new QueryString(requestTarget.substring(questionPos+1));
            if (queryString.getParameter("status") != null) {
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null) {
                body = queryString.getParameter("body");
            }
        }else if (!requestPath.equals("/echo")) {
            File file = new File(contentRoot, requestPath);
            if (!file.exists()) {
                body = file + " does not exist";

                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "\r\n" +
                        body;
                clientSocket.getOutputStream().write(response.getBytes());
                return;
            }
            statusCode = "200";
            String contentType = "text/plain";
            if (file.getName().endsWith(".html")) {
                contentType = "text/html";
            }
            if(file.getName().endsWith(".css")){
                contentType = "text/css";
            }
            String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                    "Content-Length: " + file.length() + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";
            clientSocket.getOutputStream().write(response.getBytes());
            new FileInputStream(file).transferTo(clientSocket.getOutputStream());
        }
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                body;


        clientSocket.getOutputStream().write(response.getBytes());
        //try (FileInputStream inputStream = new FileInputStream(body)) {
        //  inputStream.transferTo(clientSocket.getOutputStream());
    }*/


    //----------------

    private void handleGetMembers(Socket clientSocket) throws IOException {

        String body = "<ul>";
        for (String name : projectNames) {
            name = URLDecoder.decode(name, StandardCharsets.UTF_8.toString());
            body += "<li>" + name + "</li>";
        }
        for (String email : projectEmail) {
            email = URLDecoder.decode(email, StandardCharsets.UTF_8.toString());
            body += "<li>" + email + "</li>";
        }
        body += "</ul>";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }


    private void handleEchoRequest(Socket clientSocket, String requestTarget, int questionPos) throws IOException {
        String statusCode = "200";
        String body = "Hello <strong>World</strong>!";
        if (questionPos != -1) {
            // body=hello
            QueryString queryString = new QueryString(requestTarget.substring(questionPos + 1));
            if (queryString.getParameter("status") != null) {
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null) {
                body = queryString.getParameter("body");
            }
        }
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
    }

    //-------------------------

    public static void main(String[] args) throws IOException {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/kristianiamember");
        dataSource.setUser("kristianiashopuser");
        // TODO: database passwords should never be checked in!
        dataSource.setPassword("ssdftyklmnop");//5HGQ[f_t2D}^?
        HttpServer server = new HttpServer(8080, dataSource);
        server.setContentRoot(new File("src/main/resources"));
    }

    public void setContentRoot(File contentRoot) {
        this.contentRoot = contentRoot;
    }

    public List<String> getProductNames() throws SQLException {
        return productDao.list();
    }
}