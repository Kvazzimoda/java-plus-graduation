package ru.practicum.ewm.compilation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private CompilationService compilationService;

    @Test
    void createCompilation_WithEvents_Success() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Test Compilation");
        request.setEvents(Set.of(1L, 2L));

        Event event1 = new Event();
        event1.setId(1L);
        Event event2 = new Event();
        event2.setId(2L);
        List<Event> events = List.of(event1, event2);

        Compilation compilation = new Compilation();
        compilation.setTitle("Test Compilation");

        Compilation savedCompilation = new Compilation();
        savedCompilation.setId(1L);
        savedCompilation.setTitle("Test Compilation");

        CompilationDto expectedDto = new CompilationDto();
        expectedDto.setId(1L);
        expectedDto.setTitle("Test Compilation");

        when(eventRepository.findAllById(request.getEvents())).thenReturn(events);
        when(mapper.toCompilation(request)).thenReturn(compilation);
        when(compilationRepository.save(compilation)).thenReturn(savedCompilation);
        when(mapper.toCompilationDto(savedCompilation)).thenReturn(expectedDto);

        CompilationDto result = compilationService.createCompilation(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Compilation", result.getTitle());

        verify(eventRepository).findAllById(request.getEvents());
        verify(compilationRepository).save(compilation);
        verify(mapper).toCompilationDto(savedCompilation);
    }

    @Test
    void createCompilation_WithoutEvents_Success() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Test Compilation");
        request.setEvents(null);

        Compilation compilation = new Compilation();
        compilation.setTitle("Test Compilation");

        Compilation savedCompilation = new Compilation();
        savedCompilation.setId(1L);

        CompilationDto expectedDto = new CompilationDto();
        expectedDto.setId(1L);

        when(mapper.toCompilation(request)).thenReturn(compilation);
        when(compilationRepository.save(compilation)).thenReturn(savedCompilation);
        when(mapper.toCompilationDto(savedCompilation)).thenReturn(expectedDto);

        CompilationDto result = compilationService.createCompilation(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());

        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository).save(compilation);
    }

    @Test
    void updateCompilation_UpdateTitle_Success() {
        Long compId = 1L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Updated Title");

        Compilation existingCompilation = new Compilation();
        existingCompilation.setId(compId);
        existingCompilation.setTitle("Old Title");

        Compilation updatedCompilation = new Compilation();
        updatedCompilation.setId(compId);
        updatedCompilation.setTitle("Updated Title");

        CompilationDto expectedDto = new CompilationDto();
        expectedDto.setId(compId);
        expectedDto.setTitle("Updated Title");

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(existingCompilation)).thenReturn(updatedCompilation);
        when(mapper.toCompilationDto(updatedCompilation)).thenReturn(expectedDto);

        CompilationDto result = compilationService.updateCompilation(compId, request);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Title", existingCompilation.getTitle());

        verify(compilationRepository).findById(compId);
        verify(compilationRepository).save(existingCompilation);
    }

    @Test
    void updateCompilation_UpdatePinned_Success() {
        Long compId = 1L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setPinned(true);

        Compilation existingCompilation = new Compilation();
        existingCompilation.setId(compId);
        existingCompilation.setPinned(false);

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(existingCompilation)).thenReturn(existingCompilation);
        when(mapper.toCompilationDto(existingCompilation)).thenReturn(new CompilationDto());

        compilationService.updateCompilation(compId, request);

        assertTrue(existingCompilation.getPinned());
        verify(compilationRepository).save(existingCompilation);
    }

    @Test
    void updateCompilation_NotFound_ThrowsNotFoundException() {
        Long compId = 999L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.updateCompilation(compId, request));

        assertEquals("Подборка с ID 999 не найдена", exception.getMessage());
        verify(compilationRepository, never()).save(any());
    }

    @Test
    void deleteCompilation_Success() {
        Long compId = 1L;
        Compilation compilation = new Compilation();
        compilation.setId(compId);

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(compId);

        verify(compilationRepository).findById(compId);
        verify(compilationRepository).delete(compilation);
    }

    @Test
    void deleteCompilation_NotFound_ThrowsNotFoundException() {
        Long compId = 999L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.deleteCompilation(compId));

        assertEquals("Подборка с ID 999 не найдена", exception.getMessage());
        verify(compilationRepository, never()).delete(any());
    }

    @Test
    void getCompilationById_Success() {
        Long compId = 1L;
        Compilation compilation = new Compilation();
        compilation.setId(compId);

        CompilationDto expectedDto = new CompilationDto();
        expectedDto.setId(compId);

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(mapper.toCompilationDto(compilation)).thenReturn(expectedDto);

        CompilationDto result = compilationService.getCompilationById(compId);

        assertNotNull(result);
        assertEquals(compId, result.getId());

        verify(compilationRepository).findById(compId);
        verify(mapper).toCompilationDto(compilation);
    }

    @Test
    void getCompilationById_NotFound_ThrowsNotFoundException() {
        Long compId = 999L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.getCompilationById(compId));

        assertEquals("Подборка с ID 999 не найдена", exception.getMessage());
    }
}
