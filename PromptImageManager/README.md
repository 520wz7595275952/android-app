# 提示词图片管理器

一个功能强大的Android应用，用于存储和管理图片及其对应的AI提示词，并集成了多种AI API功能。

## 功能特点

### 基础功能
1. **图片存储** - 存储图片到应用私有目录，不会出现在系统相册中
2. **提示词管理** - 为每张图片添加、编辑和复制对应的提示词
3. **图片分类** - 支持创建分类，对图片进行分组管理
4. **搜索功能** - 支持按提示词内容搜索图片
5. **分享接收** - 可以从其他应用（如浏览器）分享图片到本应用
6. **一键复制** - 快速复制提示词到剪贴板

### AI功能
7. **AI聊天** - 调用GPT-4、Claude等API进行对话
8. **图片反推** - 使用AI分析图片生成提示词（支持OpenAI Vision、Replicate等）
9. **文生图** - 根据提示词生成图片（支持DALL-E 3、Stability AI、Banana.dev等）
10. **图生图** - 上传参考图生成相似图片
11. **视频生成** - 根据图片或提示词生成视频（支持Runway、Pika Labs、Replicate等）

## 技术栈

- Kotlin
- Android SDK 34
- Room 数据库
- Glide 图片加载
- OkHttp 网络请求
- Material Design 3

## 项目结构

```
app/src/main/java/com/promptimagemanager/
├── api/                     # API服务层
│   └── ApiService.kt        # AI API调用服务
├── data/                    # 数据层
│   ├── AppDatabase.kt       # Room数据库
│   ├── ApiConfig.kt         # API配置实体
│   ├── ApiConfigDao.kt      # API配置DAO
│   ├── Category.kt          # 分类数据类
│   ├── CategoryDao.kt       # 分类DAO
│   ├── ImageDao.kt          # 图片DAO
│   ├── ImageItem.kt         # 图片数据类
│   ├── ImageWithCategory.kt # 关联数据类
│   └── Repository.kt        # 数据仓库
├── adapter/                 # 适配器
│   ├── CategoryAdapter.kt   # 分类列表适配器
│   ├── ImageAdapter.kt      # 图片列表适配器
│   ├── ShareImageAdapter.kt # 分享图片适配器
│   └── ChatAdapter.kt       # 聊天消息适配器
├── ui/                      # UI层
│   ├── MainActivity.kt      # 主界面
│   ├── AddImageActivity.kt  # 添加图片
│   ├── ImageDetailActivity.kt # 图片详情
│   ├── CategoryActivity.kt  # 分类管理
│   ├── ShareReceiverActivity.kt # 接收分享
│   ├── ApiConfigActivity.kt # API配置管理
│   ├── ApiConfigEditActivity.kt # API配置编辑
│   ├── ChatActivity.kt      # AI聊天
│   ├── ImageGenerationActivity.kt # 图片生成
│   └── VideoGenerationActivity.kt # 视频生成
└── utils/                   # 工具类
    ├── ClipboardHelper.kt   # 剪贴板工具
    └── ImageStorageManager.kt # 图片存储管理
```

## 支持的AI服务

### 文字聊天
- OpenAI GPT-4 / GPT-3.5
- Claude 3 (Anthropic)
- comfly.chat (用户指定)
- 其他兼容OpenAI格式的API

### 图片反推（图生文）
- OpenAI GPT-4 Vision
- Replicate BLIP
- 其他Vision模型

### 图片生成（文生图/图生图）
- DALL-E 3 (OpenAI)
- Stability AI
- Banana.dev
- Replicate (Stable Diffusion等)

### 视频生成
- Runway Gen-2
- Pika Labs
- Replicate (Stable Video Diffusion等)

## 构建说明

### 方法一：使用 Android Studio（推荐）

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio，选择 "Open an existing Android Studio project"
3. 选择本项目文件夹
4. 等待 Gradle 同步完成
5. 连接 Android 设备或启动模拟器
6. 点击 "Run" 按钮（绿色三角形）或按 Shift+F10

### 方法二：使用命令行

需要安装：
- JDK 17 或更高版本
- Android SDK
- Gradle 8.2

```bash
# 进入项目目录
cd PromptImageManager

# 给予 gradlew 执行权限
chmod +x gradlew

# 下载依赖并构建 APK
./gradlew assembleDebug

# APK 输出路径
app/build/outputs/apk/debug/app-debug.apk
```

## 安装说明

构建完成后，可以通过以下方式安装：

```bash
# 使用 adb 安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

或直接传输到手机安装。

## 使用说明

### 添加图片
1. 点击右下角的 "+" 按钮
2. 选择 "从相册选择"
3. 选择要添加的图片
4. 输入提示词（可选）
5. 选择分类（可选）
6. 点击保存

### 从其他应用分享图片
1. 在其他应用（如浏览器）中打开图片
2. 点击分享按钮
3. 选择 "提示词图片管理"
4. 输入提示词和选择分类
5. 点击保存

### 配置AI API
1. 点击右上角菜单，选择 "API配置"
2. 点击右下角 "+" 添加配置
3. 选择预设或手动输入：
   - **预设**：OpenAI、Claude、DALL-E、Stability AI等
   - **手动**：输入API地址、密钥、模型名称
4. 测试配置是否可用

### AI聊天
1. 点击顶部工具栏的 "AI聊天" 图标
2. 输入消息与AI对话
3. 点击消息旁的复制按钮可复制回复

### 图片反推
1. 打开任意图片详情页
2. 点击 "图片反推" 按钮
3. 等待AI分析图片
4. 可选择使用生成的提示词或直接复制

### 生成相似图片
1. 打开任意图片详情页
2. 点击 "生成相似" 按钮
3. 可选择修改提示词
4. 点击生成，等待AI生成新图片
5. 保存生成的图片

### 文生图
1. 点击顶部工具栏的 "AI生图" 图标
2. 输入正向提示词和负向提示词（可选）
3. 调整参数（迭代步数等）
4. 点击生成
5. 保存喜欢的图片

### 图生图
1. 在图片生成页面点击 "选择参考图片"
2. 选择一张参考图
3. 输入提示词
4. 调整参考图强度（strength）
5. 点击生成

### 视频生成
1. 点击顶部工具栏的 "AI视频" 图标
2. 选择参考图片（可选）或输入提示词
3. 设置视频时长
4. 点击生成
5. 等待视频生成完成（可能需要几分钟）
6. 播放或分享生成的视频

### 管理分类
1. 点击右上角菜单，选择 "分类"
2. 点击右下角 "+" 添加新分类
3. 点击分类可查看该分类下的图片
4. 点击删除图标可删除分类

### 复制提示词
- 在图片列表中，点击 "复制提示词" 按钮
- 或在图片详情页点击 "复制" 按钮

## API配置示例

### OpenAI GPT-4
- **名称**: OpenAI GPT-4
- **类型**: 文字聊天
- **API地址**: `https://api.openai.com/v1/chat/completions`
- **API密钥**: `sk-...`
- **模型**: `gpt-4`

### DALL-E 3
- **名称**: DALL-E 3
- **类型**: 图片生成
- **API地址**: `https://api.openai.com/v1/images/generations`
- **API密钥**: `sk-...`
- **模型**: `dall-e-3`

### comfly.chat
- **名称**: Comfly Chat
- **类型**: 文字聊天
- **API地址**: `https://ai.comfly.chat/v1/chat/completions`
- **API密钥**: 你的API密钥
- **模型**: `gpt-4`

## 隐私说明

- 所有图片都存储在应用的私有目录中，不会出现在系统相册
- API密钥仅存储在本地数据库中
- 所有AI API调用直接发送到对应服务商，不经过中间服务器
- 数据完全本地存储，不会上传到任何第三方服务器

## 许可证

MIT License
