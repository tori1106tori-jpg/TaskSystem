# 1. Java17が動く軽量なLinux環境をベースにする指示
FROM eclipse-temurin:17-jdk-alpine

# 2. Renderの中でプログラムを組み立てるための作業フォルダを作成
WORKDIR /app

# 3. パソコンからアップロードされたプログラム一式をRenderの中にコピー
COPY . .

# 4. Java（Maven）の組み立てを自動実行し、公開用の実行ファイル（jar）を作成
RUN ./mvnw clean package -DskipTests

# 5. Renderの中でWebアプリを起動するポート番号（8080）を開放
EXPOSE 8080

# 6. 完成したJavaプログラムを本番起動する命令
CMD ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]
