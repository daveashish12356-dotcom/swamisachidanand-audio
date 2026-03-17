#!/usr/bin/env python3
"""
YouTube channel videos proxy - deploy to Vercel/Netlify/Railway.
Set env: YOUTUBE_API_KEY (create new key with "None" restriction for server use)
"""
import os
import json
import urllib.request
import urllib.parse

def handler(event, context=None):
    """Vercel serverless / Netlify function."""
    api_key = os.environ.get("YOUTUBE_API_KEY", "")
    if not api_key:
        return {"statusCode": 500, "body": json.dumps({"error": "YOUTUBE_API_KEY not set"})}

    params = event.get("queryStringParameters") or {}
    channel_ids = params.get("channel_ids", "")
    if not channel_ids:
        return {"statusCode": 400, "body": json.dumps({"error": "channel_ids required"})}

    ids = [x.strip() for x in channel_ids.split(",") if x.strip()]
    if not ids:
        return {"statusCode": 400, "body": json.dumps({"error": "channel_ids required"})}

    all_videos = []
    for cid in ids[:5]:  # max 5 channels
        playlist_id = "UU" + cid[2:] if cid.startswith("UC") else cid
        url = (
            "https://www.googleapis.com/youtube/v3/playlistItems"
            "?part=snippet&playlistId=" + urllib.parse.quote(playlist_id)
            + "&maxResults=15&key=" + api_key
        )
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "SwamiSachidanand/1.0"})
            with urllib.request.urlopen(req, timeout=15) as r:
                data = json.loads(r.read().decode())
        except Exception as e:
            continue
        items = data.get("items", [])
        for it in items:
            sn = it.get("snippet", {})
            rid = sn.get("resourceId", {})
            vid = rid.get("videoId")
            if not vid:
                continue
            thumbs = sn.get("thumbnails", {})
            thumb = thumbs.get("medium", {}).get("url") or thumbs.get("high", {}).get("url")
            if not thumb:
                thumb = "https://img.youtube.com/vi/" + vid + "/hqdefault.jpg"
            all_videos.append({
                "videoId": vid,
                "title": sn.get("title", ""),
                "thumbnailUrl": thumb,
                "publishedAt": sn.get("publishedAt", "")
            })

    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json", "Access-Control-Allow-Origin": "*"},
        "body": json.dumps({"videos": all_videos})
    }
