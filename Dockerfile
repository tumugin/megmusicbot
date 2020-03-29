FROM adoptopenjdk:13.0.2_8-jdk-hotspot-bionic
#FROM adoptopenjdk:14_36-jdk-hotspot-bionic

# Set environment variables
ENV FFMPEG_PATH="/usr/bin/ffmpeg" \
    DB_CONNECTION="jdbc:sqlite:megmusicbot.db"

# Install dependency packages(FFmpeg).
RUN apt update && \
    apt install -y ffmpeg

# Copy Source Codes.
COPY . /usr/src
WORKDIR /usr/src

# Build megmusicbot.
# Generated Binary:
# /usr/src/build/libs/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar
RUN /usr/src/gradlew shadowJar

#/usr/src/build/libs/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar
