# ComputerFolderService
MvvmSample's server
**Project Description**

This project is a macOS application that allows mobile devices to access photos stored on the Mac. Users can select which photos are accessible from the mobile device. Support for accessing various files and videos will be added in future updates.
<img width="1102" height="861" alt="image" src="https://github.com/user-attachments/assets/9fd6a16a-1d92-4ab5-a179-17662804e887" />
## 功能说明

### 1. IP 与端口设置
- **IP 一般无需修改**，保持默认即可。
- **端口**在默认端口被占用时需要手动更改。

### 2. 根目录选择
- 可通过旁边的 **目录选择按钮** 进行选择，或者直接输入路径。
- **建议使用按钮选取**，以避免路径输入错误。
- 该目录用于限定之后可选取图片的最大范围。
- 之后选取的图片必须位于此目录或其子目录中。

### 3. 已选图片区域
- 所有被勾选的图片都会显示在这一行。
- 可以在此处删除不需要的图片。
- 最终返回给客户端访问的图片列表即来源于此区域中的图片。

### 4. 子目录按钮区域
- 在根目录下存在子目录时，会显示一行蓝紫色按钮。
- 点击按钮可进入对应子目录。
- 如果当前目录没有子目录，这一行不会出现。

### 5. 图片网格展示区域
- 以网格形式展示当前目录下的所有图片。
- 勾选图片后，它们会出现在第 3 条描述的“已选图片区域”中。

### 6. 服务启动与停止
- **启动按钮**：打开服务，使客户端可以访问刚才选中的图片。
- **停止按钮**：关闭服务，客户端将无法访问这些图片。



