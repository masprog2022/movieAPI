package com.masprogtech.services;

import com.masprogtech.dto.MovieDto;
import com.masprogtech.entities.Movie;
import com.masprogtech.exceptions.FileExistsException;
import com.masprogtech.exceptions.MovieNotFoundException;
import com.masprogtech.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService{

    private final MovieRepository movieRepository;

    private final FileService fileService;

    @Value("${project.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;


    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {

        // 1. upload the file
        if (Files.exists(Paths.get(path + File.separator + file.getOriginalFilename()))){
            throw new FileExistsException("File already exists! please enter another file name!");
        }

        String uploadedFileName = fileService.uploadFile(path, file);

        // 2. set the value of field 'poster' as filename
        movieDto.setPoster(uploadedFileName);

        // 3. map dto to Movie object
        Movie movie = new Movie(
                null,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        // 4. save the movie object -> saved Movie object
        Movie savedMovie = movieRepository.save(movie);

        // 5. generate the posterUrl
        String posterUrl = baseUrl + "/file/" + uploadedFileName;

        // 6. map Movie object to DTO object and return it

        return new MovieDto(
                savedMovie.getMovieId(),
                savedMovie.getTitle(),
                savedMovie.getDirector(),
                savedMovie.getStudio(),
                savedMovie.getMovieCast(),
                savedMovie.getReleaseYear(),
                savedMovie.getPoster(),
                posterUrl
        );
    }

    @Override
    public MovieDto getMovie(Integer movieId) {
        // 1. check the data in DB and if exists, fetch the data of given ID

       Movie movie =  movieRepository.findById(movieId).orElseThrow(
                ()-> new MovieNotFoundException("Movie does not exists!")
        );
        // 2. generate posterUrl
        String posterUrl = baseUrl + "/file/" + movie.getPoster();

        // 3. map to MovieDto object and return it

        return new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );

    }

    @Override
    public List<MovieDto> getAllMovies() {

        // 1. fetch all data from DB
        List<Movie> movies = movieRepository.findAll();

        List<MovieDto> movieDtos = new ArrayList<>();

        // 2. iterate through the list, generate posterUrl for each movie obj,
        // and map to MovieDto obj
        for(Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            movieDtos.add(movieDto);
        }

        return movieDtos;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {

        // 1. check if movie object exists with given movieId

        Movie mv = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found!"));

        // 2. if file is null, do nothing
        // if file is not null, then delete existing file associated with the record,
        // and upload the new file

        String fileName = mv.getPoster();
        if(file != null){
            Files.deleteIfExists(Paths.get(path + File.separator + fileName));
            fileName = fileService.uploadFile(path, file);
        }

        // 3. set movieDto's poster value, according to step 2
        movieDto.setPoster(fileName);

        // 4. map it to Movie object
        Movie movie = new Movie(
                mv.getMovieId(),
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        // 5. save the movie object -> return saved movie object

        String posterUrl = baseUrl + "/file/" + fileName;

        // 7. map to MovieDto and return it
        return new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );

    }

    @Override
    public String deleteMovie(Integer movieId) throws IOException {

        // 1. check if movie object exists in DB

        Movie mv = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found!"));
        Integer id = mv.getMovieId();

        // 2. delete the file associated with this object

        Files.deleteIfExists(Paths.get(path + File.separator + mv.getPoster()));

        // 3. delete the movie object
        movieRepository.delete(mv);

        return "Movie deleted with id = " + id;
    }


}
