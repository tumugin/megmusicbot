# megmusicbot
## セットアップ
### 1. DBのセットアップ(マイグレーションの実行)
```bash
./gradlew jarmonicaUp
```
DBはデフォルトで同梱しているsqliteを使用するようになっています。

`src/main/kotlin/db/config/Default.kt` を編集することでこれらの設定を変えることが出来ます。

### 2. コンフィグの設置
`config.sample.json` を参考に `config.json` を同じ階層に作ってください。

DBの接続設定はマイグレーションを実行した環境と同じにしてください。

### 3. 曲のスキャンの実行
```bash
./gradlew run --args='--scanner'
```

### 4. Botの起動
```bash
./gradlew run --args='--bot'
```
