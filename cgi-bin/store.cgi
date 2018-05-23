#!/usr/bin/env python

#print("Content-Type: text/html\n\n")
import os
import sys
import cgi, cgitb
import Cookie
cgitb.enable()

def writeHTML(content):
    sys.stdout.write("<html>\n<body>" + content + "</body>\n</html>")


com = (os.environ.get("REQUEST_METHOD"))

if com == 'GET':
	print("""
		<html> 
			<head>
				<title>hello</title>
			</head>
			<body>
				<p>Please enter your login information</p>
				<form action="/cgi-bin/store.cgi" method="POST">
					Username:<br>
					<input type = "text" name="user"><br>
					Password:<br>
					<input type="text" name="pass"><br><br>
					<input type="submit" value="login">
				</form>
			</body>
		</html> """)
	sys.exit()
elif com == 'POST':
	
	
	#get user name
	form = cgi.FieldStorage()
	user = form.getvalue('user')
	if os.environ.get("HTTP_COOKIE"):
		cookieString = os.environ.get("HTTP_COOKIE")
		#print(cookieString)
		c=Cookie.SimpleCookie()
		c.load(cookieString)
		try:
			user=c['user'].value
		except KeyError:
			user = form.getvalue('user')



	#Represents user's cart by storing cookie values.Item not in cart if a[0] = ""
	a = ["","","",""]
	#items in store. Initially set to full
	b = ["ls3","filter","vtec","golf"]
	#does not get changed
	masterList = ["ls3","filter","vtec","golf"]
	
	added = form.getvalue('add')
	rem = form.getvalue('rmr')
	
	if os.environ.get("HTTP_COOKIE"):
		#get cookies and update lists
		for i in range(4):
			cookieString = os.environ.get("HTTP_COOKIE")
			c=Cookie.SimpleCookie()
			c.load(cookieString)
			try:
				a[i] = c[masterList[i]].value
			except KeyError:
				a[i] = ""
			if a[i] == "":
				#no cart cookie, add item to store list
				b[i] = masterList[i]
				
			else:
				#cart cookie, remove item from store list
				b[i] = ""
	#item added to cart, update list & cookies
	if added:
		if added == "ls3":
			a[0] = 'ls3'
			b[0] = "" 
			
			os.putenv("HTTP_COOKIE", "ls3=ls3")
		elif added == "filter":
			a[1] = 'filter'
			b[1] = ""
			
			os.putenv("HTTP_COOKIE", "filter=filter")
		elif added == "vtec":
			a[2] = "vtec"
			b[2] = ""
			
			os.putenv("HTTP_COOKIE", "vtec=vtec")

		elif added =="golf":
			a[3] == "golf"
			b[3] = ""
			
			os.putenv("HTTP_COOKIE", "golf=golf")
		else:
			#do nothing
			print('')
	
	#item removed from cart, update lists & cookies
	if rem:
		if rem == "ls3":
			a[0] = ""
			b[0] = "ls3" 
			
			#os.unsetenv("ls3")
		elif rem == "filter":
			a[1] = ""
			b[1] = "filter"
			
			#os.unsetenv("filter")
		elif rem == "vtec":
			a[2] = ""
			b[2] = "vtec"
			
			#os.unsetenv("vtec")

		elif rem =="golf":
			a[3] == ""
			b[3] = "golf"
			
			#os.unsetenv("golf")
		else:
			#do nothing
			print('')

	#generate product form w/ only available products
	strProdForm = """ 
	<form id="list" action="/cgi-bin/store.cgi" method="POST">
		<select name="add" form="list">\n	
	"""
	
	prodForm = ""
	cartForm = ""
		
	#get product list, and create elements product and cart forms
	if b[0] != "":
		prodForm+="""<option value="ls3">LS3 Crate Motor $6000</option>\n"""
	else:
		cartForm+="""<option value="ls3">LS3 Crate Motor $6000</option>\n"""
	
	if b[1] != "":
		prodForm+="""<option value="filter">K&N High Flow Filter $20</option>\n"""
	else:
		cartForm+="""<option value="filter">K&N High Flow Filter $20</option>\n"""
	
	if b[2] != "":
		prodForm+="""<option value="vtec">VTEC Bumper Sticker $5</option>\n"""
	else:
		cartForm+="""<option value="vtec">VTEC Bumper Sticker $5</option>\n"""
	
	if b[3] != "":
		prodForm+="""<option value="golf">Golf Ball Shift Knob $10</option>\n"""
	else:
		cartForm+="""<option value="golf">Golf Ball Shift Knob $10</option>\n"""

	endProdForm = """ 
	</select>
		<input type ="submit" value="add to cart">
	</form>\n"""
	products = "<p>Hello {user}!</p><br>\n"
	if not any(b):
		products += "<p>No Products Available</p>\n"
	else:
		products += strProdForm+prodForm+endProdForm
	
	#create cart form
	userCart = "<br>\n"

	strCartForm = """ 
	<form id="cart" action="/cgi-bin/store.cgi" method="POST">
		<select name="rmr" form="cart">\n	
	"""

	endCartForm = """ 
	</select>
		<input type ="submit" value="remove">
	</form>\n"""
	
	if not any(a):
		userCart+="<p>Your Cart is Empty</p>"
	else:
		userCart+="<p>Your Cart</p><br>"
		userCart += strCartForm+cartForm+endCartForm
	
	
	#send completed page 
	body = products+userCart
	writeHTML(body.format(user=user))
	sys.exit()
else:
	print('no method')
	sys.exit();


