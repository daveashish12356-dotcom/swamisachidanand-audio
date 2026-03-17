# Add Chanakya Rajniti to server and push. Run from repo root: .\push_chanakya_rajniti.ps1
Set-Location $PSScriptRoot
git add public/books_server_list.json
git add add_chanakya_rajniti_to_server.py
git add public/books/
git add public/thumbnails/
git status -s public/
git commit -m "Server: add Chanakya ni Rajniti book PDF + thumbnail + list"
git push origin main
Write-Host "Done. Book on server; app will show after refresh."
