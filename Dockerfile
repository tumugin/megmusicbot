## Build Container
FROM adoptopenjdk:13.0.2_8-jdk-hotspot-bionic as build
#FROM adoptopenjdk:14_36-jdk-hotspot-bionic

# Install dependency packages(FFmpeg).
RUN apt update && \
    apt install -y xz-utils

# Copy Source Codes.
COPY . /usr/src
WORKDIR /usr/src

# Build megmusicbot.
# Generated Binary:
# /usr/src/build/libs/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar
RUN /usr/src/gradlew shadowJar

RUN curl -LO https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz && \
    tar -xvf ffmpeg*.tar.xz && \
    mv ffmpeg-*/ffmpeg /usr/src/ffmpeg && \
    rm -rf ffmpeg-* && \
    ls -la

## Production Container
FROM adoptopenjdk:14_36-jre-hotspot-bionic

# Set environment variables
ENV FFMPEG_PATH="/usr/bin/ffmpeg" \
    DB_CONNECTION="jdbc:sqlite:megmusicbot.db"

# Add megmusic user
RUN useradd -b / -m megmusic
USER megmusic
RUN mkdir /megmusic/data
WORKDIR /megmusic/data

# Copy jar and FFmpeg from build stage.
COPY --from=build /usr/src/build/libs/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar /megmusic
COPY --from=build /usr/src/ffmpeg /usr/bin

# Create entrypoint script.
RUN echo '#!/bin/sh' >> /megmusic/entrypoint.sh && \
    echo 'java -jar $1 --db-migrate' >> /megmusic/entrypoint.sh && \
    echo 'if [ $? != 0 ] ; then' >> /megmusic/entrypoint.sh && \
    echo '    exit 1' >> /megmusic/entrypoint.sh && \
    echo 'fi' >> /megmusic/entrypoint.sh && \
    echo 'java -jar $1 --scanner' >> /megmusic/entrypoint.sh && \
    echo 'if [ $? != 0 ] ; then' >> /megmusic/entrypoint.sh && \
    echo '    exit 1' >> /megmusic/entrypoint.sh && \
    echo 'fi' >> /megmusic/entrypoint.sh && \
    echo 'exec java -jar $1' >> /megmusic/entrypoint.sh && \
    chmod +x /megmusic/entrypoint.sh

# Set Entrypoint.
ENTRYPOINT [ "/megmusic/entrypoint.sh" ]
CMD [ "/megmusic/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar" ]
