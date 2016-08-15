var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);
var bodyParser = require('body-parser');

app.use(bodyParser.json());

server.listen(3000);

app.get('/accelerometer', function(req, res){
  res.sendFile(__dirname + '/accel.html');
});

app.post('/accelerometer', function(req, res) {
  var data = req.body;
  console.log(req.body);
  io.emit('data event', { accel: data });
});




