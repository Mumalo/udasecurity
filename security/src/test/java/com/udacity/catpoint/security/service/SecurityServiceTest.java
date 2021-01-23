package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    Sensor sensor;

    @Mock
    SecurityRepository securityRepository;

    @Mock
    ImageService imageService;

    private
    SecurityService securityService;



    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    @ParameterizedTest //covers 1 and 2 test cases
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeAlarmStatus_alarmArmedAndSensorActivated_alarmStatusPending(ArmingStatus armingStatus){
        when(sensor.getActive()).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(getSensors(true, 2));
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.PENDING_ALARM);

        //alarm was already pending
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(captor.capture()); //first call up
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test //tests 3
    void changeAlarmStatus_alarmPendingAndAllSensorsInactive_changeToNoAlarm(){
        Set<Sensor> allSensors = getSensors(false, 4);
        when(securityRepository.getSensors()).thenReturn(allSensors);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @Test  //tests 4
    void changeAlarmState_alarmActiveAndSensorStateChanges_stateNotAffected() {
        when(sensor.getActive()).thenReturn(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(securityRepository, atMostOnce()).updateSensor(captor.capture());
        assertEquals(captor.getValue(), sensor);

        //when sensor state changes
        when(sensor.getActive()).thenReturn(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        verify(securityRepository, atMost(2)).updateSensor(captor.capture());
        assertEquals(captor.getValue(), sensor);

    }

    @ParameterizedTest
    @MethodSource("booleanMethodSource")
    void changeAlarmStatus_sensorStatusChangeAndSystemIsAlreadyDisarmed_stateNotAffected(boolean b1, boolean b2){
        when(sensor.getActive()).thenReturn(b1);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, b2);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(securityRepository, atMostOnce()).updateSensor(captor.capture());
        assertEquals(captor.getValue(), sensor);
    }

    static Stream<Arguments> booleanMethodSource(){
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(false, true)
        );
    }

    private Set<Sensor> getSensors(boolean active, int count){
        String randomString = UUID.randomUUID().toString();

        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i <= count; i++){
            sensors.add(new Sensor(randomString, SensorType.DOOR));
        }
        sensors.forEach(it -> it.setActive(active));
        return sensors;
    }

    @Test //tests 5
    void changeAlarmState_systemActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState(){
        when(sensor.getActive()).thenReturn(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM); //fix code
    }

    @ParameterizedTest //tests 6
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void changeAlarmState_sensorDeactivateWhileInactive_noChangeToAlarmState(AlarmStatus alarmStatus){
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test //tests 7
    void changeAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test //tests 8
    void changeAlarmState_noCatImageIdentifiedAndSensorsAreInactive_changeToAlarmStatus(){
        Set<Sensor> inactiveSensors = getSensors(false, 4);
        when(securityRepository.getSensors()).thenReturn(inactiveSensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @Test //tests 9
    void changeAlarmStatus_systemDisArmed_changeToAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    //If the system is armed, reset all sensors to inactive.
    @ParameterizedTest //tests 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void updateSensors_systemArmed_deactivateAllSensors(ArmingStatus armingStatus){
        Set<Sensor> sensors = getSensors(true, 4);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        List<Executable> executables = new ArrayList<>();
        sensors.forEach(it -> executables.add(() -> assertEquals(it.getActive(), false)));
        assertAll(executables);
    }

    @Test
    void changeAlarmStatus_systemArmedHomeAndCatDetected_changeToAlarmStatus(){
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }


}