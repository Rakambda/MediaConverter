package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.AvifConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class PhotoToAvifMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("jpeg");
	
	@Override
	public boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file) {
		if(Objects.isNull(probeResult)){
			return file.getFileName().toString().endsWith(".HEIC");
		}
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier) {
		return new AvifConverter(input, output, temporary);
	}
	
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "avif";
	}
}
