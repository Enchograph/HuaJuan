# 设置要统计的文件夹路径
$folderPath = "D:\Moi\Projects\Github\HuaJuan4\app\src\main\"
# 设置要统计的文件扩展名
$extensions = @("*.cs","*.java","*.py","*.js","*.ts","*.cpp","*.c","*.php","*.go","*.rs","*.swift","*.kt","*.html","*.css","*.rb","*.scala","*.pl","*.sh","*.bat","*.ps1","*.xml")

Write-Host "正在统计文件夹: $folderPath" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan

# 获取所有符合条件的文件
$files = Get-ChildItem -Path $folderPath -Recurse -File -Include $extensions

$totalFiles = 0
$totalLines = 0
$results = @()

foreach ($file in $files) {
    try {
        $lines = (Get-Content $file.FullName -ErrorAction Stop | Measure-Object -Line).Lines
        $totalLines += $lines
        $totalFiles++

        $results += [PSCustomObject]@{
            Lines = $lines
            Name = $file.Name
            Path = $file.FullName
            Extension = $file.Extension
            LastWrite = $file.LastWriteTime
            Size = "{0:N2} KB" -f ($file.Length / 1KB)
        }
    }
    catch {
        Write-Warning "无法读取文件: $($file.FullName)"
    }
}

# 按行数排序并显示
Write-Host "`n前20个最大的代码文件:" -ForegroundColor Yellow
$results | Sort-Object Lines -Descending | Select-Object -First 20 | Format-Table -AutoSize -Property @{Name="行数";Expression={$_.Lines};Align="Right"}, Name, Extension, Size, LastWrite, @{Name="相对路径";Expression={$_.Path.Replace($folderPath, ".")}}

Write-Host "`n统计摘要:" -ForegroundColor Yellow
Write-Host "总文件数: $totalFiles" -ForegroundColor Cyan
Write-Host "总代码行数: $totalLines" -ForegroundColor Cyan
Write-Host "平均每个文件行数: {0:N0}" -f ($totalLines / [Math]::Max(1, $totalFiles)) -ForegroundColor Cyan

# 按扩展名分组统计
Write-Host "`n按文件类型统计:" -ForegroundColor Yellow
$results | Group-Object Extension | ForEach-Object {
    $sum = ($_.Group | Measure-Object -Property Lines -Sum).Sum
    $count = $_.Count
    [PSCustomObject]@{
        扩展名 = $_.Name
        文件数 = $count
        总行数 = $sum
        平均行数 = "{0:N0}" -f ($sum / $count)
    }
} | Sort-Object 总行数 -Descending | Format-Table -AutoSize