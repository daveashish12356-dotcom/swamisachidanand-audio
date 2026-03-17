# Push gallery + app changes. Run when no other git process is running.
# If you get "index.lock" error: close Cursor/VS Code git panels, then run this script again.

Set-Location $PSScriptRoot

# Remove stale lock if any (only if no other git is running)
if (Test-Path ".git\index.lock") {
    Remove-Item ".git\index.lock" -Force -ErrorAction SilentlyContinue
}

git add public/gallery
git add app/src/main/AndroidManifest.xml
git add app/src/main/java/com/swamisachidanand/PhotoGalleryActivity.java
git add app/src/main/java/com/swamisachidanand/PhotoGalleryAdapter.java
git add app/src/main/java/com/swamisachidanand/HomeFragment.java
git add app/src/main/java/com/swamisachidanand/BookStoreActivity.java
git add app/src/main/res/layout/fragment_home.xml
git add app/src/main/res/layout/activity_photo_gallery.xml
git add app/src/main/res/layout/item_photo_gallery.xml

git status --short public/ app/src/main/

git commit -m "Add photo gallery: public/gallery + app PhotoGallery page, Book Store new-section fix, home gallery link"
git push origin main

Write-Host "Done. If push asked for login, use browser or token as per your GitHub setup."
