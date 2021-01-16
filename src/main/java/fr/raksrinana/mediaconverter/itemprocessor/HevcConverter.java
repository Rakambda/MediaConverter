package fr.raksrinana.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.raksrinana.mediaconverter.utils.ProgressBarNotifier;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
@Slf4j
public class HevcConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final Path temporary;
	
	public HevcConverter(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary){
		super(input, output);
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.temporary = temporary.toAbsolutePath().normalize();
	}
	
	@Override
	public void run(){
		var filename = getOutput().getFileName().toString();
		
		var duration = Optional.ofNullable(probeResult.getFormat())
				.map(Format::getDuration)
				.map(Float::longValue)
				.map(Duration::ofMillis)
				.orElse(Duration.ZERO);
		var durationStr = String.format("%dh%dm%s", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
		var frameCount = probeResult.getStreams().stream().mapToLong(Stream::getNbFrames).max().orElse(0);
		
		log.info("Converting {} ({}) to {}", getInput(), durationStr, getOutput());
		try{
			log.debug("Will convert to temp file {}", temporary);
			ffmpeg.addInput(UrlInput.fromPath(getInput()))
					.addOutput(UrlOutput.toPath(temporary)
							.setCodec(StreamType.AUDIO, "aac")
							.addArguments("-b:a", "128000")
							.setCodec(StreamType.VIDEO, "libx265")
							.addArguments("-preset", "medium")
							.addArguments("-crf", "23")
							.addArguments("-movflags", "use_metadata_tags")
							.addArguments("-map_metadata", "0")
							.addArguments("-max_muxing_queue_size", "512")
					)
					.setOverwriteOutput(false)
					.setProgressListener(new ProgressBarNotifier(filename, frameCount, durationStr))
					.execute();
			
			if(Files.exists(temporary)){
				Files.move(temporary, getOutput());
				
				copyFileAttributes(getInput(), getOutput());
				trashFile(getInput());
				
				log.info("Converted {} to {}", getInput(), getOutput());
			}
			else{
				log.warn("Output file {} not found, something went wrong", getOutput());
			}
		}
		catch(IOException e){
			log.error("Failed to run ffmpeg on {}", getInput(), e);
		}
	}
}
