package fr.rakambda.mediaconverter.itemprocessor;

import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
public abstract class CommandConverter extends ConverterRunnable{
	private final ProgressBarSupplier converterProgressBarSupplier;
	
	private Process process;
	
	protected CommandConverter(@NonNull Path input, @NonNull Path output, @NonNull Path temporary, boolean deleteInput, ProgressBarSupplier converterProgressBarSupplier){
		super(input, output, temporary, deleteInput);
		this.converterProgressBarSupplier = converterProgressBarSupplier;
	}
	
	protected abstract String[] getCommand();
	
	@Override
	protected Future<?> convert(@NonNull ExecutorService executorService, boolean dryRun){
		log.info("Converting {} to {}", getInput(), getOutput());
		
		return executorService.submit(() -> {
			if(dryRun){
				log.info("Dry run: would have executed `{}`", String.join(" ", getCommand()));
				return;
			}
			try(var progressBar = converterProgressBarSupplier.get()){
				progressBar.getProgressBar().stepTo(0);
				progressBar.getProgressBar().setExtraMessage(getOutput().getFileName().toString());
				progressBar.getProgressBar().maxHint(1);
				
				ProcessBuilder builder = new ProcessBuilder(getCommand());
				process = builder.start();
				process.onExit()
						.thenAccept(p -> progressBar.getProgressBar().stepTo(1))
						.thenAccept(p -> close());
				process.waitFor();
			}
			catch(Exception e){
				log.error("Failed to convert", e);
			}
		});
	}
	
	@Override
	public void cancel(){
		if(Objects.nonNull(process) && process.isAlive()){
			process.destroy();
		}
	}
	
	@Override
	public void close(){
	}
}
