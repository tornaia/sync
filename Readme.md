## General information

This project is just proof of concept to demonstrate that <tt style="color:red">file synchronization</tt> is not necessarily super difficult and can be fast.
Remember: it is just a PoC and not something ready for production use.

##### Dependencies

* Java: JDK8
* Maven: 3.3.9
* Jubos/fake-s3 for localhost development and e2e tests
	* https://github.com/jubos/fake-s3
	* http://railsapps.github.io/openssl-certificate-verify-failed.html
* MongoDB for localhost development and e2e tests
	* install it as a windows service
	    * `$ mongod --install`
	* create root:root user
	    * `$ mongo --port 27017`
	    * `$ use admin`
	    * `$ db.createUser( { user: "root", pwd: "root", roles: [ "root" ] } )`
	
##### IntelliJ IDEA setup

* How to start local server
    * Add New Configuration
	* Name: server-localhost
	* Main class: com.github.tornaia.sync.server.ServerApp
	* Program arguments: --spring.profiles.active=real-mongo-local,jubos-s3-faker
	* Use classpath of module: sync-server
	
* How to start client against local server
    * Add New Configuration
	* Name: client-localhost-1
	* Main class: com.github.tornaia.sync.client.win.WinClientApp
	* Program arguments: --spring.profiles.active=localhost-1
	* Use classpath of module: sync-client-win

* How to start client against cloud-deployed server
    * Add New Configuration
	* Name: client-cloud-1
	* Main class: com.github.tornaia.sync.client.win.WinClientApp
	* Program arguments: --spring.profiles.active=cloud-1
	* Use classpath of module: sync-client-win