mkdir output_test
for /R "output" %%f in (*.proto) do Z:\protoc-3.20.0-win64\bin\protoc.exe -I=%cd%\output --java_out=output_test\ "%%f"
