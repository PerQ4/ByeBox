@echo off
set PATH=C:\Program Files\Go\bin;C:\Users\perqa\go\bin;%PATH%
set ANDROID_HOME=C:\Users\perqa\AppData\Local\Android\Sdk
set ANDROID_NDK_HOME=C:\Users\perqa\AppData\Local\Android\Sdk\ndk\android-ndk-r27c
set GOPATH=C:\Users\perqa\go

echo Clones AndroidLibXrayLite repository...
if not exist xray-build (
    git clone https://github.com/2dust/AndroidLibXrayLite.git xray-build
)

cd xray-build
echo Tidying Go modules...
call go mod tidy -v

if exist build rmdir /S /Q build
echo Building libv2ray.aar...
call gomobile bind -v -o libv2ray.aar -target android -androidapi 24 -ldflags="-checklinkname=0 -s -w" ./

echo Copying AAR to app/libs...
copy /Y libv2ray.aar ..\app\libs\libv2ray.aar

echo Done!
