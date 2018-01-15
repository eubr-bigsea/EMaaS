from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
import cgi
import re
import json
import urlparse
import subprocess
 
response = [
    {'id': 1, 'title': 'Running BULMA application...'},
]

class RestHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):

        if None != re.search('/bulma/execute/', self.path):
            url = urlparse.urlparse(self.path)
            values = urlparse.parse_qs(url.query)
            
            token = values.get("token")[0]
            spark = values.get("spark")[0]
            classTag =  " --class "
            classPath = values.get("classPath")[0]
            memory = " --executor-memory " + values.get("memory")[0]
            numCores = " --total-executor-cores " + values.get("cores")[0]
            mesosPath = " --master " + values.get("mesos")[0]
            jarPath = values.get("jarPath")[0]
            shapePath = values.get("shapePath")[0]
            gpsPath = values.get("gpsPath")[0]
            outputPath = values.get("outputPath")[0]
            numPartitions = values.get("numPartitions")[0]

            command = spark + classTag + classPath + memory + numCores + mesosPath + " " + jarPath + " " + shapePath + " " + gpsPath + " " + outputPath + " " + numPartitions
            print command
            subprocess.call(command, shell=True)

            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps({'data': response}))
	    	
        return
 
 
httpd = HTTPServer(('0.0.0.0', 8050), RestHTTPRequestHandler)
while True:
    httpd.handle_request()