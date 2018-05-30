#  CS352: Web-Server

The main function of this program is to simulate a small scale HTTP 1.0 server that can be accessed by a browser. This web server supports concurrency, the handling of different MIME types, as well as support for cookies and the creation of dynamic content via CGI scripts. When running the program, the port number is the single arguement the program takes. The running server can than be accessed by entering the IP address of the machine running the program along with the port number in a web browser. 

# Supported Commands
The web server provides support for HTTP GET, HEAD, and POST methods, as well as additional support for cookies. All requests must be in the proper format for HTTP version 1.1 or lower. Any incorrectly formatted request will result in the server returning either a HTTP 400 or HTTP 505 error. In addition, the server will display the following status codes:
200 OK
304 Not Modified
400 Bad Request
403 Forbidden
404 Not Found
500 Internal Server
501 Not Implemented
505 HTTP Version Not Supported.

In addition, if a POST request does not contain a Content-Length header, the server will return a 411 Length required status code. Any POST requested not targed to a CGI script will return with a 405 Method Not allowed header. 

# Script Support
There are two CGI scripts, Service.CGI, and Store.CGI. The service script provides an MD5 validation service. When the script is run, a webpage will be displayed that will allow a user to upload a file and enter an MD5 hash. The program will then create a dynamic webpage indicating if the resulting hash entered matches the hash of the file. 

Store.cgi is a simple webstore, in which a user must first login to access the webstore. When adding an item to the cart, items are removed from the store until they are removed from a users cart. Cookie support means that their cart will not empty even after they log out. 
