package com.adamrosenfield.wordswithcrosses.net;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.adamrosenfield.wordswithcrosses.BrowseActivity;
import com.adamrosenfield.wordswithcrosses.PlayActivity;
import com.adamrosenfield.wordswithcrosses.WordsWithCrossesApplication;
import com.adamrosenfield.wordswithcrosses.io.IO;
import com.adamrosenfield.wordswithcrosses.puz.Puzzle;
import com.adamrosenfield.wordswithcrosses.puz.PuzzleMeta;
import com.adamrosenfield.wordswithcrosses.wordswithcrosses.R;

public class Downloaders {
    private static final Logger LOG = Logger.getLogger("com.adamrosenfield.wordswithcrosses");
    private Context context;
    private List<Downloader> downloaders = new LinkedList<Downloader>();
    private NotificationManager notificationManager;
    private boolean suppressMessages;

    public Downloaders(SharedPreferences prefs,
            NotificationManager notificationManager, Context context) {
        this.notificationManager = notificationManager;
        this.context = context;

        if (prefs.getBoolean("downloadGlobe", true)) {
            downloaders.add(new BostonGlobeDownloader());
        }

        if (prefs.getBoolean("downloadThinks", true)) {
            downloaders.add(new ThinksDownloader());
        }
        if (prefs.getBoolean("downloadWaPo", true)) {
         downloaders.add(new WaPoDownloader());
         }

        if (prefs.getBoolean("downloadWsj", true)) {
            downloaders.add(new WSJDownloader());
        }

        if (prefs.getBoolean("downloadWaPoPuzzler", true)) {
            downloaders.add(new WaPoPuzzlerDownloader());
        }

        if (prefs.getBoolean("downloadNYTClassic", true)) {
            downloaders.add(new NYTClassicDownloader());
        }

        if (prefs.getBoolean("downloadInkwell", true)) {
            downloaders.add(new InkwellDownloader());
        }

        if (prefs.getBoolean("downloadJonesin", true)) {
            downloaders.add(new JonesinDownloader());
        }

        if (prefs.getBoolean("downloadLat", true)) {
//          downloaders.add(new UclickDownloader("tmcal", "Los Angeles Times", "Rich Norris", Downloader.DATE_NO_SUNDAY));
            downloaders.add(new LATimesDownloader());
        }

        if (prefs.getBoolean("downloadAvClub", true)) {
            downloaders.add(new AVClubDownloader());
        }

        if (prefs.getBoolean("downloadPhilly", true)) {
            downloaders.add(new PhillyDownloader());
        }

        if (prefs.getBoolean("downloadCHE", true)) {
            downloaders.add(new CHEDownloader());
        }

        if (prefs.getBoolean("downloadJoseph", true)) {
            downloaders.add(new KFSDownloader("joseph", "Joseph Crosswords",
                    "Thomas Joseph", Downloader.DATE_NO_SUNDAY));
        }

        if (prefs.getBoolean("downloadSheffer", true)) {
            downloaders.add(new KFSDownloader("sheffer", "Sheffer Crosswords",
                    "Eugene Sheffer", Downloader.DATE_NO_SUNDAY));
        }

        if (prefs.getBoolean("downloadPremier", true)) {
            downloaders.add(new KFSDownloader("premier", "Premier Crosswords",
                    "Frank Longo", Downloader.DATE_SUNDAY));
        }

        if (prefs.getBoolean("downloadNewsday", true)) {
            downloaders.add(new UclickDownloader("crnet", "Newsday",
                    "Stanley Newman, distributed by Creators Syndicate, Inc.",
                    Downloader.DATE_DAILY));
        }

        if (prefs.getBoolean("downloadUSAToday", true)) {
            downloaders.add(new UclickDownloader("usaon", "USA Today",
                    "USA Today", Downloader.DATE_NO_SUNDAY));
        }

        if (prefs.getBoolean("downloadUniversal", true)) {
            downloaders.add(new UclickDownloader("fcx", "Universal Crossword",
                    "uclick LLC", Downloader.DATE_DAILY));
        }

        if (prefs.getBoolean("downloadLACal", true)) {
            downloaders.add(new UclickDownloader("lacal",
                    "LAT Sunday Calendar", "Los Angeles Times",
                    Downloader.DATE_SUNDAY));
        }

        if (prefs.getBoolean("downloadISwear", true)) {
            downloaders.add(new ISwearDownloader());
        }

        if (prefs.getBoolean("downloadNYT", false)) {
            downloaders.add(new NYTDownloader(context, prefs.getString(
                    "nytUsername", ""), prefs.getString("nytPassword", "")));
        }

        this.suppressMessages = prefs.getBoolean("suppressMessages", false);
    }

    public List<Downloader> getDownloaders(Calendar date) {
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        List<Downloader> retVal = new LinkedList<Downloader>();

        for (Downloader d : downloaders) {
            if (arrayContains(d.getDownloadDates(), dayOfWeek)) {
                retVal.add(d);
            }
        }

        return retVal;
    }

    private static boolean arrayContains(int[] array, int key) {
        for (int x : array) {
            if (x == key) {
                return true;
            }
        }

        return false;
    }

    public void download(Calendar date) {
        download(date, getDownloaders(date));
    }

    public void download(Calendar date, List<Downloader> downloaders) {
        date = (Calendar)date.clone();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        String contentTitle = context.getResources().getString(R.string.downloading_puzzles);

        Notification not = new Notification(android.R.drawable.stat_sys_download, contentTitle,
                System.currentTimeMillis());
        boolean somethingDownloaded = false;

        File crosswordsDir = WordsWithCrossesApplication.CROSSWORDS_DIR;
        File archiveDir = WordsWithCrossesApplication.ARCHIVE_DIR;

        if (!WordsWithCrossesApplication.makeDirs()) {
            return;
        }

        HashSet<File> newlyDownloaded = new HashSet<File>();

        int i = 1;
        for (Downloader d : downloaders) {
            d.setContext(context);

            try {
                String contentText = context.getResources().getString(R.string.downloading_from);
                contentText = contentText.replace("${SOURCE}", d.getName());
                Intent notificationIntent = new Intent(context, PlayActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                not.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

                if (!this.suppressMessages && this.notificationManager != null) {
                    this.notificationManager.notify(0, not);
                }

                File downloaded = new File(crosswordsDir, d.createFileName(date));
                File archived = new File(archiveDir, d.createFileName(date));

                System.out.println(downloaded.getAbsolutePath() + " " + downloaded.exists() + " OR " +
                    archived.getAbsolutePath() + " " + archived.exists());

                if (downloaded.exists() || archived.exists()) {
                    continue;
                }

                downloaded = d.download(date);

                if (downloaded == Downloader.DEFERRED_FILE) {
                    continue;
                }

                if (downloaded != null) {
                    boolean updatable = false;

                    PuzzleMeta meta = new PuzzleMeta();
                    meta.date = date;
                    meta.source = d.getName();
                    meta.sourceUrl = d.sourceUrl(date);
                    meta.updateable = updatable;

                    if (processDownloadedPuzzle(downloaded, meta)) {
                        if (!this.suppressMessages) {
                            this.postDownloadedNotification(i, d.getName(), downloaded);
                        }

                        newlyDownloaded.add(downloaded);
                        somethingDownloaded = true;
                    }
                }

                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }

        if (somethingDownloaded) {
            this.postDownloadedGeneral();
        }
    }

    public static boolean processDownloadedPuzzle(File downloaded,
            PuzzleMeta meta) {
        try {
            System.out.println("==PROCESSING " + downloaded + " hasmeta: "
                    + (meta != null));

            Puzzle puz = IO.load(downloaded);
            if(puz == null){
                return false;
            }
            puz.setDate(meta.date);
            puz.setSource(meta.source);
            puz.setSourceUrl(meta.sourceUrl);
            puz.setUpdatable(meta.updateable);

            IO.save(puz, downloaded);

            return true;
        } catch (Exception ioe) {
            LOG.log(Level.WARNING, "Exception reading " + downloaded, ioe);
            downloaded.delete();

            return false;
        }
    }

    public void suppressMessages(boolean b) {
        this.suppressMessages = b;
    }

    private void postDownloadedGeneral() {
        String contentTitle = context.getResources().getString(R.string.downloaded_new_puzzles);
        Notification not = new Notification(
                android.R.drawable.stat_sys_download_done, contentTitle,
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(Intent.ACTION_EDIT, null,
                context, BrowseActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
        not.setLatestEventInfo(context, contentTitle,
                "New puzzles were downloaded.", contentIntent);

        if (this.notificationManager != null) {
            this.notificationManager.notify(0, not);
        }
    }

    private void postDownloadedNotification(int i, String name, File puzFile) {
        String contentTitle = "Downloaded " + name;
        Notification not = new Notification(
                android.R.drawable.stat_sys_download_done, contentTitle,
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(Intent.ACTION_EDIT,
                Uri.fromFile(puzFile), context, PlayActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
        not.setLatestEventInfo(context, contentTitle, puzFile.getName(),
                contentIntent);

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }

//  private void postUpdatedNotification(int i, String name, File puzFile) {
//      String contentTitle = "Updated " + name;
//      Notification not = new Notification(
//              android.R.drawable.stat_sys_download_done, contentTitle,
//              System.currentTimeMillis());
//      Intent notificationIntent = new Intent(Intent.ACTION_EDIT,
//              Uri.fromFile(puzFile), context, PlayActivity.class);
//      PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
//              notificationIntent, 0);
//      not.setLatestEventInfo(context, contentTitle, puzFile.getName(),
//              contentIntent);
//
//      if ((this.notificationManager != null) && !suppressMessages) {
//          this.notificationManager.notify(i, not);
//      }
//  }

}
