1. Clone this repository and enter:
	cd /emmas-api 

2. This creates a Docker image:
	docker build -t emmas-api .

3. Run the app to test:
	docker run -p 10060:80 emmas-api

4. Create a Docker hub account at https://hub.docker.com/

5. To share your image, log in to Docker and enter your username and password:
	docker login

6. To control the version, tag the image
	docker tag emmas-api <your username>/emmas-api:v0

7. Upload your tagged image to the repository:
	docker push <your username>/emmas-api:v0

That's it! Your image is now available for use on other machines with the 3rd step or Marathon.

Links: https://docs.docker.com/get-started/part2/
