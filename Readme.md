## Project setup

This project is just proof of concept to demonstrate that file synchronization is not necessarily super difficult and can be fast.
Remember: it is just a PoC and not something ready for production use.

##### Install

* Java: JDK8

* Maven: 3.3.9

* Jubos/fake-s3 for localhost development and e2e tests
	- https://github.com/jubos/fake-s3
	- http://railsapps.github.io/openssl-certificate-verify-failed.html

* MongoDB for localhost development and e2e tests

	- to install it as a windows service:
	$ mongod --install

	- run mongo as service (no manual start needed)
	http://stackoverflow.com/questions/2438055/how-to-run-mongodb-as-windows-service
	login to mongo:
	$ mongo --port 27017

	- create root:root user 
	$ use admin
	$ db.createUser( { user: "root", pwd: "root", roles: [ "root" ] } )
	
##### IDEA configuration

- How to start local server (see sync-server/src/main/resources/application-*.properties)
    * Add New Configuration
	* Name: server-localhost
	* Main class: com.github.tornaia.sync.server.ServerApp
	* Program arguments: --spring.profiles.active=real-mongo-local,jubos-s3-faker
	* Use classpath of module: sync-server
	
- How to start client against local server (see sync-client-win/src/main/resources/application-localhost-n.properties)
    * Add New Configuration
	* Name: client-localhost-1
	* Main class: com.github.tornaia.sync.client.win.WinClientApp
	* Program arguments: --spring.profiles.active=localhost-1
	* Use classpath of module: sync-client-win

- How to start client against cloud-deployed server (see sync-client-win/src/main/resources/application-cloud-n.properties)
    * Add New Configuration
	* Name: client-cloud-1
	* Main class: com.github.tornaia.sync.client.win.WinClientApp
	* Program arguments: --spring.profiles.active=cloud-1
	* Use classpath of module: sync-client-win

	

	