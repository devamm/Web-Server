#!/usr/bin/env python3

#print("Content-Type: text/html\n\n")
import os
import sys
import cgi
import hashlib


def writeHTML(content):
    sys.stdout.write("<html>\n<body>" + content + "</body>\n</html>")

def sendForm():
	body = """   
    <br>
    <p>Please upload a file and a MD5 hash</p>
    <br>
    <form action="service.cgi" method="POST" enctype="multipart/form-data">
    	Enter a MD5 hash:<input type="text" name="hash"><br>
    	Select a file: <input type="file" name = filename >
    	<br>
    	<input type="submit" value="Submit">
    </form>
    """
	writeHTML(body)

com = (os.environ.get("REQUEST_METHOD"))

if com == 'GET':
	#emit html form for file and md5
	sendForm()
	sys.exit()
else:
	len = 0
	try:
		len = int(os.environ.get("CONTENT_LENGTH", ""))
	except ValueError:
	    #no content, display page with form for file and md5
		sendForm()
		sys.exit()
	
	payload = sys.stdin.read(len)
	line = payload.index('\r\n')
	boundary = payload[:line]
	
	#separates payload by boundary
	sections = payload.split(boundary)
	#look for blank line
	x = sections[1].index('\r\n\r\n')
	hashC = sections[1][x+4:]
	#isolate md5 value, strip off \r\n from end, do same steps for file contents
	hashC = hashC[:-2]

	i = sections[2].index('\r\n\r\n')
	fileC = sections[2][i+4:]
	fileC = fileC[:-2]
	
	#hash file
	hashObj = hashlib.md5(fileC.encode())
	hashedFile = hashObj.hexdigest()

	#print(hashedFile)
	if hashedFile == hashC.lower(): 
		body ='<br>\n<center>\n<p>Hashes are the same!</p>\n<br>\n<img src="http://i.imgur.com/QDR6NDp.gif">\n</center>'
	else:
		body='<br>\n<center>\n<p>Hashes do not match</p>\n<br>\n<img src="http://i.imgur.com/RjPPL9B.gif">\n</center>'
	
	writeHTML(body)
	
