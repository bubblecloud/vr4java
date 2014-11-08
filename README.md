vr4java
=======

Virtual reality for Java

Heroku Server Setup
-------------------

1) Clone vr4java to local machine.

```
git clone https://github.com/bubblecloud/vr4java.git
```

2) Execute heroku server setup in local vr4java root folder

```
cd vr4java
heroku apps:create <hostname> --region <eu/us>
heroku config:add java_opts='-Xmx384m -Xms384m -Xss512k -XX:+UseCompressedOops'
heroku config:add JAVA_OPTS='-Xmx384m -Xms384m -Xss512k -XX:+UseCompressedOops'
git push heroku master
```

3) Login with browser to https://<hostname>