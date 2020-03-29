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
./gradlew run --args='--bot'
```
