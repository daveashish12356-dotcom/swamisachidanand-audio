using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using SwamiDesktop.Models;

namespace SwamiDesktop;

public partial class MainWindow : Window
{
    private readonly HttpClient _http = new HttpClient();

    // Android app jaisa hi live server base URL
    private const string ServerBaseUrl = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/";

    public MainWindow()
    {
        InitializeComponent();
    }

    #region Books

    private async void OnLoadBooksClicked(object sender, RoutedEventArgs e)
    {
        BooksStatusText.Text = "પુસ્તકો લોડ થઈ રહ્યા છે...";
        BooksList.ItemsSource = null;

        try
        {
            var books = await LoadBooksAsync();
            BooksList.ItemsSource = books;
            BooksStatusText.Text = $"કુલ પુસ્તકો: {books.Count}";
        }
        catch (Exception ex)
        {
            BooksStatusText.Text = "ભૂલ આવી (વિગત Output વિંડોમાં જુઓ)";
            Debug.WriteLine("LoadBooks error: " + ex);
        }
    }

    private async Task<List<Book>> LoadBooksAsync()
    {
        var url = ServerBaseUrl.TrimEnd('/') + "/public/books_server_list.json";
        var json = await _http.GetStringAsync(url);

        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;
        var list = new List<Book>();

        if (!root.TryGetProperty("fileNames", out var files))
            return list;

        foreach (var el in files.EnumerateArray())
        {
            var fileName = el.GetString() ?? "";
            if (!fileName.EndsWith(".pdf", StringComparison.OrdinalIgnoreCase))
                continue;

            var displayName = Path.GetFileNameWithoutExtension(fileName);
            var encodedPdf = Uri.EscapeDataString(fileName).Replace("+", "%20");
            var thumbName = Path.GetFileNameWithoutExtension(fileName) + ".jpg";
            var encodedThumb = Uri.EscapeDataString(thumbName).Replace("+", "%20");

            var booksBase = ServerBaseUrl.TrimEnd('/') + "/public/books/";
            var thumbsBase = ServerBaseUrl.TrimEnd('/') + "/public/thumbnails/";

            list.Add(new Book
            {
                Name = displayName,
                FileName = fileName,
                PdfUrl = booksBase + encodedPdf,
                ThumbnailUrl = thumbsBase + encodedThumb + "?v=2"
            });
        }

        return list;
    }

    private void BooksList_OnMouseDoubleClick(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        if (BooksList.SelectedItem is not Book book) return;
        if (string.IsNullOrWhiteSpace(book.PdfUrl)) return;

        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = book.PdfUrl,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            MessageBox.Show("PDF ખોલી શક્યા નહીં.\n" + ex.Message,
                "Error", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    #endregion

    #region Audio

    private async void OnLoadAudioClicked(object sender, RoutedEventArgs e)
    {
        AudioStatusText.Text = "ઓડિયો યાદી લોડ થઈ રહી છે...";
        AudioBooksList.ItemsSource = null;
        AudioPartsList.ItemsSource = null;

        try
        {
            var books = await LoadAudioBooksAsync();
            AudioBooksList.ItemsSource = books;
            AudioStatusText.Text = $"ઓડિયો પુસ્તકો: {books.Count}";
        }
        catch (Exception ex)
        {
            AudioStatusText.Text = "ભૂલ આવી (વિગત Output માં જુઓ)";
            Debug.WriteLine("LoadAudioBooks error: " + ex);
        }
    }

    private async Task<List<AudioBook>> LoadAudioBooksAsync()
    {
        var baseUrl = ServerBaseUrl.TrimEnd('/');
        var candidates = new[]
        {
            baseUrl + "/public/audio_list.json?v=8",
            baseUrl + "/public/audio_list_main_updated.json",
            baseUrl + "/public/audio_list_main.json"
        };

        string json = "";
        Exception? lastError = null;
        foreach (var url in candidates)
        {
            try
            {
                json = await _http.GetStringAsync(url);
                if (!string.IsNullOrWhiteSpace(json))
                {
                    Debug.WriteLine("Loaded audio JSON from " + url);
                    break;
                }
            }
            catch (Exception ex)
            {
                lastError = ex;
            }
        }

        if (string.IsNullOrWhiteSpace(json))
        {
            throw lastError ?? new Exception("Audio JSON લોડ થયું નથી.");
        }

        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;
        var list = new List<AudioBook>();

        if (!root.TryGetProperty("books", out var booksEl))
            return list;

        foreach (var b in booksEl.EnumerateArray())
        {
            var id = b.TryGetProperty("id", out var idEl) ? idEl.GetString() ?? "" : "";
            var title = b.TryGetProperty("title", out var tEl) ? tEl.GetString() ?? "" : "";
            var parts = new List<AudioPart>();

            if (b.TryGetProperty("parts", out var partsEl))
            {
                foreach (var p in partsEl.EnumerateArray())
                {
                    var pid = p.TryGetProperty("id", out var pidEl) ? pidEl.GetString() ?? "" : "";
                    var ptitle = p.TryGetProperty("title", out var ptEl) ? ptEl.GetString() ?? "" : "";
                    var url = p.TryGetProperty("url", out var uEl) ? uEl.GetString() ?? "" : "";
                    if (string.IsNullOrWhiteSpace(url)) continue;
                    parts.Add(new AudioPart { Id = pid, Title = ptitle, Url = url });
                }
            }

            if (parts.Count == 0) continue;

            list.Add(new AudioBook
            {
                Id = id,
                Title = title,
                Parts = parts
            });
        }

        return list;
    }

    private void AudioBooksList_OnSelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
    {
        if (AudioBooksList.SelectedItem is not AudioBook book)
        {
            AudioPartsList.ItemsSource = null;
            return;
        }
        AudioPartsList.ItemsSource = book.Parts;
    }

    private void OnPlaySelectedAudioPartClicked(object sender, RoutedEventArgs e)
    {
        if (AudioPartsList.SelectedItem is not AudioPart part) return;
        if (string.IsNullOrWhiteSpace(part.Url)) return;

        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = part.Url,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            MessageBox.Show("ઓડિયો ચલાવી શક્યા નહીં.\n" + ex.Message,
                "Error", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    #endregion
}