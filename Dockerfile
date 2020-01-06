FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/FranCali/SudokuP2P.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/SudokuP2P /app
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=1 /app/target/SudokuP2P-1.0.jar /app

CMD /usr/bin/java -cp SudokuP2P-1.0.jar it.unisa.studenti.App -id $ID -m $MASTERIP