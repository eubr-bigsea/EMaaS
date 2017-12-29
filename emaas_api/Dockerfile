FROM zptdba/ubuntu16-java8

RUN apt-get update && apt-get install -y python-pip
RUN apt-get update && apt-get install -y curl

RUN curl -s https://d3kbcqa49mib13.cloudfront.net/spark-2.1.0-bin-hadoop2.7.tgz | tar -xz -C /usr/local/
RUN cd /usr/local && ln -s spark-2.1.0-bin-hadoop2.7 spark
ENV SPARK_HOME /usr/local/spark
RUN cd /usr/local/spark/python && pip install -e .

#install pycurl
RUN apt-get update
RUN apt-get install python-pycurl

# install ssh
# RUN pip install paramiko
# RUN apt-get update && apt-get install -y openssh-server
# RUN mkdir /var/run/sshd
# RUN echo 'root:screencast' | chpasswd
# RUN sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config
# RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
# ENV NOTVISIBLE "in users profile"
# RUN echo "export VISIBLE=now" >> /etc/profile

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY bulma-api.py .
COPY requirements.txt .

CMD [ "python", "bulma-api.py" ]