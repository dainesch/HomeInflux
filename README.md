# HomeInflux
HomeInflux collects data from a set of plugins and stores it in influxdb. There are already many really good projects out there that work in the same way, but this one is designed to be simple to use and has a set of plugins that could be helpful for home use. 

![Grafana displaying the collected data](./docs/grafana-tumb.png?raw=true)

[Grafana displaying the collected data](./docs/grafana.png?raw=true)

# Plugins

* Afterburner plugin: Collects CPU/GPU temps and many other statistics from MSI Afterburner Remote Server 
* Hue Sensor plugin: reads the available data from your Hue sensors (temp, light, presence)
* Fritz.Box plugin: reads the upstream and downstream history from your Fritz Box (TR064)
* SNMP plugin: reads the desired SNMP OIDs
* Ping Plugin: ping a set of hosts

# How to use

1. Build the project using maven. The jar will be located in the ./target directory and the required libraries in ./target/lib
```
mvn install
```
2. Run it a first time (config file location optional). On the first run it will generate a sample config file and exit
```
java -jar HomeInflux-1.0.jar [/path/to/config/file.json]
```
3. Edit the sample config file and enable/configure the plugins
4. Run it again

## Note on the Hue plugin
If you enable the plugin, you have to specify the IP adress of the hue bridge. You do not have yet the required access key, 
that is fine and normal. if now you run HomeInflux it will prompt you to press the Hue bridge button and it will print the required 
key on the console. You will then have to copy paste this key in your config file.
