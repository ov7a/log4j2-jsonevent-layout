## What is it?

This is a layout for log4j2 that generates logstash json_event formatted data, which supposed to be drop in replacement for `JSONEventLayoutV1` from https://github.com/logstash/log4j-jsonevent-layout

## Usage example

```
<Socket name="logstash" host="your_host" port="43000" protocol="udp">
  <JSONEventLayoutV2 />
</Socket>
```
or 

```
<Socket name="logstash" host="your_host" port="43000" protocol="udp">
  <JSONEventLayoutV2 UserFields="foo:bar,baz:qux"/>
</Socket>
```

You might need to add package info to `Configuration` node.
Note that userFields from command-line aren't implemented. 

## Licence
No garantees. Do whatever you want with this.

