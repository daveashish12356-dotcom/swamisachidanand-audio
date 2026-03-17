using System.Collections.Generic;

namespace SwamiDesktop.Models;

public class AudioBook
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public List<AudioPart> Parts { get; set; } = new();
}

public class AudioPart
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Url { get; set; } = "";
}

