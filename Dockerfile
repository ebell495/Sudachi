FROM gradle:jdk11
COPY . /Sudachi
WORKDIR /Sudachi
RUN gradle installExecutableDist
WORKDIR /Sudachi/build/install/sudachi-executable
RUN wget http://sudachi.s3-website-ap-northeast-1.amazonaws.com/sudachidict/sudachi-dictionary-20211220-core.zip && \
    unzip sudachi-dictionary-20211220-core.zip && \
    cp sudachi-dictionary-20211220/system_core.dic . && \
    find . -name "sudachi*.jar" -exec cp {} "sudachi.jar" \;
