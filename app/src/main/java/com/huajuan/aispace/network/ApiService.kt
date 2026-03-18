package com.huajuan.aispace.network

// 注意：这里的 Message 类与 Repository 中的 Message 类不同
// 支持多模态内容（文本和图片）
data class Message(
    val role: String,
    val content: Any // 支持字符串或内容数组
)

// 内容项数据类（支持文本和图片）
data class ContentItem(
    val type: String, // "text" 或 "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

// 图片URL数据类
data class ImageUrl(
    val url: String
)

// 便利构造函数，用于创建仅文本的消息
fun createTextMessage(role: String, content: String): Message {
    return Message(role, content)
}

// 便利构造函数，用于创建带图片的消息
fun createImageMessage(role: String, text: String, imageUris: List<String>): Message {
    val contentList = mutableListOf<ContentItem>()
    // 添加文本内容
    contentList.add(ContentItem(type = "text", text = text))
    
    // 添加图片内容
    for (imageUri in imageUris) {
        // 对于本地文件，需要先上传或转换为base64
        val imageUrl = if (imageUri.startsWith("http") || imageUri.startsWith("data:")) {
            imageUri  // 已经是URL或data URL
        } else {
            imageUri
        }
        contentList.add(ContentItem(type = "image_url", image_url = ImageUrl(url = imageUrl)))
    }
    
    return Message(role, contentList)
}
