#配置C预编译宏 设置不适用OpenSSL
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO" )

#所有c源文件放入 librtmp 变量
file(GLOB librtmp *.c)

#编译静态库
add_library(rtmp STATIC ${librtmp} )