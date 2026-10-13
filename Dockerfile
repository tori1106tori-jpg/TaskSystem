# 1. 最初からJavaとMaven（組み立て工具）が両方入っている公式環境を使う指示
FROM maven:3.8.4-openjdk-17-slim AS build

# 2. 作業フォルダを作成
WORKDIR /app

# 3. プログラム一式をコピー
COPY . .

# 4. mvn コマンドで安全にビルドを実行
RUN mvn clean package -DskipTests

# 5. 動かす用の軽量なJava環境を新しく用意
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# 6. targetフォルダの中にある「.jar」ファイルを名前を問わず全てコピー
COPY --from=build /app/target/*.jar app.jar

# 7. ポート番号を開放
EXPOSE 8080

# 8. 本番起動
CMD ["java", "-jar", "app.jar"]
