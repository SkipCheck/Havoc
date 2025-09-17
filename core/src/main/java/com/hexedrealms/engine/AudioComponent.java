package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.hexedrealms.configurations.AudioConfiguration;
import de.pottgames.tuningfork.Audio;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;
import de.pottgames.tuningfork.StreamedSoundSource;
import de.pottgames.tuningfork.jukebox.JukeBox;
import de.pottgames.tuningfork.jukebox.JukeBoxObserver;
import de.pottgames.tuningfork.jukebox.playlist.DefaultPlayListProvider;
import de.pottgames.tuningfork.jukebox.playlist.PlayList;
import de.pottgames.tuningfork.jukebox.song.Song;
import de.pottgames.tuningfork.jukebox.song.SongMeta;
import de.pottgames.tuningfork.jukebox.song.SongSettings;

public class AudioComponent implements JukeBoxObserver {
    public enum MusicState {
        NONE,
        EXPLORATION,
        COMBAT,
        BOSS
    }

    private static AudioComponent instance;

    private final Audio audio;
    private float currentMusicVolume = AudioConfiguration.MUSIC.getValue();
    private float currentCommonVolume = AudioConfiguration.COMMON.getValue();
    private MusicState currentMusicState = MusicState.NONE;
    public MusicState targetState;

    // Main music
    private JukeBox mainJukeBox;
    private StreamedSoundSource mainSoundSource;
    private PlayList mainPlayList;

    // Combat music
    private JukeBox combatJukeBox;
    private StreamedSoundSource combatSoundSource;
    private PlayList combatPlayList;

    // Sound effects
    public SoundBuffer openDoor;
    public SoundBuffer knockDoor;
    public SoundBuffer secretDoom;
    public SoundBuffer secret;

    private AudioComponent() {
        audio = Audio.init();
        loadSoundEffects();
    }

    private void loadSoundEffects() {
        openDoor = SoundLoader.load(Gdx.files.internal("audio/sounds/doors/open.wav"));
        knockDoor = SoundLoader.load(Gdx.files.internal("audio/sounds/doors/knock.wav"));
        secretDoom = SoundLoader.load(Gdx.files.internal("audio/sfx/doom-secret.wav"));
        secret = SoundLoader.load(Gdx.files.internal("audio/sfx/secret.wav"));
    }

    public static AudioComponent getInstance() {
        if (instance == null) {
            instance = new AudioComponent();
        }
        return instance;
    }

    public void loadMainMusic(String filename) {
        cleanupJukeBox(mainJukeBox, mainSoundSource);

        mainSoundSource = new StreamedSoundSource(Gdx.files.internal(filename));
        mainSoundSource.setRelative(true);

        Song song = new Song(mainSoundSource,
            SongSettings.linear(1f, 20f, 1f),
            new SongMeta().setTitle("main_music"));

        mainPlayList = new PlayList() {
            @Override
            public String toString() {
                return "MainPlayList";
            }
        };
        mainPlayList.addSong(song);
        mainPlayList.setLooping(true);

        mainJukeBox = createJukeBox(mainPlayList);
    }

    public void loadCombatMusic(String filename) {
        cleanupJukeBox(combatJukeBox, combatSoundSource);

        combatSoundSource = new StreamedSoundSource(Gdx.files.internal(filename));
        combatSoundSource.setRelative(true);

        Song song = new Song(combatSoundSource,
            SongSettings.linear(1f, 20f, 1f),
            new SongMeta().setTitle("combat_music"));

        combatPlayList = new PlayList() {
            @Override
            public String toString() {
                return "CombatPlayList";
            }
        };
        combatPlayList.addSong(song);
        combatPlayList.setLooping(true);

        combatJukeBox = createJukeBox(combatPlayList);
    }

    private JukeBox createJukeBox(PlayList playList) {
        JukeBox jukeBox = new JukeBox(new DefaultPlayListProvider().add(playList));
        jukeBox.addObserver(this);
        jukeBox.setVolume(currentMusicVolume);
        return jukeBox;
    }

    private void cleanupJukeBox(JukeBox jukeBox, StreamedSoundSource source) {
        if (jukeBox != null) {
            jukeBox.stop();
            jukeBox.clear();
        }
        if (source != null) {
            source.dispose();
        }
    }

    public void setMusicState(MusicState newState) {

        if(currentMusicState == newState) return;

        // Жёсткая остановка всей музыки
        if (mainJukeBox != null && currentMusicState == MusicState.EXPLORATION) mainJukeBox.pause();
        if (combatJukeBox != null && currentMusicState == MusicState.COMBAT) combatJukeBox.pause();

        currentMusicState = newState;

        switch (newState) {
            case EXPLORATION:
                if (mainJukeBox != null) {
                    mainJukeBox.setVolume(currentMusicVolume); // Принудительно на максимум для теста
                    mainJukeBox.stop();
                    mainJukeBox.play();
                    Gdx.app.log("Audio", "Main music started");
                }
                break;

            case COMBAT:
            case BOSS:
                if (combatJukeBox != null) {
                    combatJukeBox.setVolume(currentMusicVolume); // Принудительно на максимум для теста
                    combatJukeBox.stop();
                    combatJukeBox.play();
                    Gdx.app.log("Audio", "Combat music started");
                }
                break;
        }
    }

    public Audio getAudio() {
        return audio;
    }

    public void setMusicVolume(float volume) {
        currentMusicVolume = volume;
        if (mainJukeBox != null) {
            mainJukeBox.setVolume(volume);
        }
        if (combatJukeBox != null) {
            combatJukeBox.setVolume(volume);
        }
    }

    public void setCommonVolume(float volume) {
        currentCommonVolume = volume;
        audio.setMasterVolume(volume);
    }

    public MusicState getCurrentMusicState() {
        return currentMusicState;
    }

    public void update() {
        if (mainJukeBox != null) {
            mainJukeBox.update();
        }
        if (combatJukeBox != null) {
            combatJukeBox.update();
        }
    }

    public void clear(){
        currentMusicState = MusicState.NONE;
        cleanupJukeBox(mainJukeBox, mainSoundSource);
        cleanupJukeBox(combatJukeBox, combatSoundSource);
    }

    public void dispose() {
        cleanupJukeBox(mainJukeBox, mainSoundSource);
        cleanupJukeBox(combatJukeBox, combatSoundSource);

        openDoor.dispose();
        knockDoor.dispose();
        secretDoom.dispose();
        secret.dispose();

        if (audio != null) {
            audio.dispose();
        }

        instance = null;
    }

    @Override
    public void onSongStart(Song song) {}
    @Override
    public void onSongEnd(Song song) {}
    @Override
    public void onPlayListStart(PlayList playList) {}
    @Override
    public void onPlayListEnd(PlayList playList) {}
    @Override
    public void onJukeBoxStart() {}
    @Override
    public void onJukeBoxPause() {}
    @Override
    public void onJukeBoxEnd() {}
}
