# <tt style="color:limegreen">Sync</tt> - File synchronization

<tt style="color:limegreen">Sync</tt> is just proof of concept to demonstrate that <tt style="color:limegreen">file synchronization</tt> is not necessarily super difficult and can be fast.
Remember: it is just a PoC and not something ready for production use.

##### Dependencies

* Java: JDK8
* Maven: 3.3.9
* Jubos/fake-s3 for localhost development and e2e tests
	* Install Ruby: http://rubyinstaller.org/
	* Install jubos/fake-s3
	   * https://github.com/jubos/fake-s3
	   * `$ gem install fakes3`
	   * If you have SSL problems: https://gist.github.com/luislavena/f064211759ee0f806c88
* MongoDB for localhost development and e2e tests
	* Install it as a windows service
	    * Create config file: C:\Program Files\MongoDB\Server\3.2\bin\mongod.cfg
	   	~~~
		bind_ip = 127.0.0.1
		dbpath = C:\mongodb\data\db
		logpath = C:\mongodb\log\mongo-server.log
		verbose=v
		~~~
	    * Create data and log directories
	    	* C:\mongodb\data\db\
		* C:\mongodb\log\
	    * `$ mongod --config "C:\Program Files\MongoDB\Server\3.2\bin\mongod.cfg" --install`
	* Create root:root user
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
