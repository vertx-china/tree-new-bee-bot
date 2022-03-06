# 转发 TreeNewBee 消息的Telegram(或其他app) Bot

## Feature
- 转发 TreeNewBee 消息到 Telegram 群
- 转发 Telegram 群消息到 TreeNewBee 

## TODO
- 图片、表情等的转发

## 使用：
### Telegram Bot 准备
1. 在 Telegram 私聊 [BotFather](https://t.me/botfather) ，创建Bot，记下 token
2. 将Bot拉入群，设为管理员（管理权限可以都去掉，只是为了能读取到所以消息）
3. 群内发消息
4. 访问 `https://api.telegram.org/bot{你的token}/getUpdates` 可以在响应里面看到刚发的消息，里面有chat的id，即群ID，记下备用。

### 启动Bot
1. `mvn clean package`
2. 编辑一个 `config.json` 配置文件:
```json
{
  "telegram.chatId": 555555555555, /*请替换为 Telegram 群ID*/
  "telegram.botToken": "44444444444:xxxxxxxxxxxxxxx 请替换为bot的token",
  "treeNewBee.server": "请替换为 TreeNewBee 服务地址",
  "treeNewBee.port": 32167 /*请替换为 TreeNewBee 端口*/
}
```
3. 启动 `java -jar target/tree-new-bee-bot-***.jar -conf config.json`