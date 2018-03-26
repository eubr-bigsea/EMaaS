from flask import Flask, redirect, request
import subprocess
import pycurl
import StringIO
 
app = Flask(__name__)
 
@app.route('/success/<msg>')
def success(msg):
    return "SUCCESS: " +  msg

@app.route('/token/<token>')
def isTokenValid(token):
    response = StringIO.StringIO()
    c = pycurl.Curl()
    c.setopt(c.URL, '<URL>')
    c.setopt(c.WRITEFUNCTION, response.write)
    c.setopt(c.POSTFIELDS, 'token=' + token)
    c.perform()
    c.close()
    print response.getvalue()
    response.close()

    return "SUCCESS: " +  response.getValue() 

#http://<IP>:10060/bulma/execute?token=a&spark=/opt/spark-1.6.3-bin-hadoop2.6/bin/spark-submit&classPath=BULMA.MatchingRoutesShapeGPS&memory=6g&cores=15&mesos=mesos://<IP>:5050&jarPath=/home/users/dmestre/EMaaS/BULMA1.6.jar&shapePath=hdfs://10.0.0.9:9000/users/dmestre/EMaaS/shapesCuritiba.csv&gpsPath=hdfs://10.0.0.9:9000/users/dmestre/EMaaS/GPS_5days/&outputPath=hdfs://10.0.0.9:9000/users/dmestre/EMaaS/output5days/outBULMA15cores/&numPartitions=60
@app.route('/bulma/execute/', methods = ['GET'])
def executeBulma(): 
    token = request.args.get('token')
    spark = request.args.get('spark')
    classTag = " --class "
    classPath = request.args.get('classPath')
    memory = " --executor-memory " + request.args.get('memory')
    numCores = " --total-executor-cores " + request.args.get('cores')
    mesosPath = " --master " + request.args.get('mesos')
    jarPath = request.args.get('jarPath')
    shapePath = request.args.get('shapePath')
    gpsPath = request.args.get('gpsPath')
    outputPath = request.args.get('outputPath')
    numPartitions = request.args.get('numPartitions')
    
    if (isTokenValid(token)):
        sparkCmd = spark + classTag + classPath + memory + numCores + mesosPath + " " + jarPath + " " + shapePath + " " + gpsPath + " " + outputPath + " " + numPartitions
        subprocess.call(sparkCmd, shell=True)
        return redirect('success/Running BULMA application...')
    
    else:
        return redirect('success/Permission denied! Check your token...')


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)