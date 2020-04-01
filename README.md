# megmusicbot
## セットアップ
### 1. コンフィグの設置
`sample.env` を参考に `.env` を同じ階層に作ってください。

### 2. DBのセットアップ(マイグレーションの実行)
```bash
./gradlew run --args='--db-migrate'
```
DBはデフォルトで同梱しているsqliteを使用するようになっています。

### 3. 曲のスキャンの実行
```bash
./gradlew run --args='--scanner'
```

### 4. Botの起動
```bash
./gradlew run
```

`--bot` は付けても付けなくても動きます。デフォルトのモードが `--bot` です。

## Docker上での利用
### 1. docker-compose.yml の編集
`<YOUR_API_KEY>` をDiscordのAPIキーに置き換えます。

### 2. Docker Containerのビルド
```bash
docker-compose build
```

### 3. DBのセットアップ(マイグレーションの実行)
```bash
docker-compose run --rm megmusic /megmusic/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar --db-migrate
```

### 4. 曲のスキャンの実行
```bash
docker-compose run --rm megmusic /megmusic/com.myskng.megmusicbot-1.0-FAIRY_STARS-all.jar --scanner
```

### 5. Botの起動
```bash
docker-compose up -d
```
