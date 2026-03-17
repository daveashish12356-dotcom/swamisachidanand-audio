# Run in a NEW terminal (outside Cursor) to avoid git lock.
# Book store only - no audio/PDF changes.

Set-Location $PSScriptRoot

# Remove stale lock
if (Test-Path ".git\index.lock") { Remove-Item ".git\index.lock" -Force }

Write-Host "Adding book store files..."
git add `
  public/books_store.json `
  public/books_store.html `
  public/index.html `
  public/book_covers/ `
  copy_book_covers.py `
  app/src/main/assets/books_store_list.json `
  app/src/main/assets/book_covers/ `
  app/src/main/java/com/swamisachidanand/BookStoreActivity.java `
  app/src/main/java/com/swamisachidanand/BookStoreAdapter.java `
  app/src/main/java/com/swamisachidanand/BookStoreItem.java `
  app/src/main/res/layout/activity_book_store.xml `
  app/src/main/res/layout/item_book_store_card.xml `
  app/src/main/res/layout/item_book_store_grid.xml

# fragment_home and AndroidManifest - needed for BookStore, add only if they have BookStore-related changes
git add app/src/main/res/layout/fragment_home.xml app/src/main/AndroidManifest.xml

Write-Host "Committing..."
git commit -m "Add Surat Book Center: 151 books, covers, server page, app integration"

Write-Host "Pushing..."
git push origin

Write-Host "Installing APK..."
adb install -r app\build\outputs\apk\debug\app-debug.apk

Write-Host "Done."
