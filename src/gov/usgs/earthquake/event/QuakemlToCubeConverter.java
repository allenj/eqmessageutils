package gov.usgs.earthquake.event;

import gov.usgs.earthquake.cube.CubeDelete;
import gov.usgs.earthquake.cube.CubeEvent;
import gov.usgs.earthquake.cube.CubeMessage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import org.quakeml_1_2rc3.Comment;
import org.quakeml_1_2rc3.CreationInfo;
import org.quakeml_1_2rc3.EvaluationMode;
import org.quakeml_1_2rc3.Event;
import org.quakeml_1_2rc3.EventType;
import org.quakeml_1_2rc3.Magnitude;
import org.quakeml_1_2rc3.Origin;
import org.quakeml_1_2rc3.OriginDepthType;
import org.quakeml_1_2rc3.OriginQuality;
import org.quakeml_1_2rc3.OriginUncertainty;
import org.quakeml_1_2rc3.OriginUncertaintyDescription;
import org.quakeml_1_2rc3.Quakeml;
import org.quakeml_1_2rc3.EventParameters;
import org.quakeml_1_2rc3.RealQuantity;
import org.quakeml_1_2rc3.TimeQuantity;

/**
 * Convert from Quakeml to CubeMessage
 * 
 * @author jmfee
 */
public class QuakemlToCubeConverter {

	/**
	 * Convert a Quakeml object to a CubeMessage object.
	 * 
	 * @param message
	 *            the quakeml message.
	 * @return CubeMessage representation of Quakeml.
	 * @throws Exception
	 */
	public CubeMessage convertQuakeml(final Quakeml message) throws Exception {
		EventParameters eventParameters = message.getEventParameters();

		List<Event> events = eventParameters.getEvents();
		if (events.size() == 0) {
			// no events to convert
			return null;
		}

		Event event = events.get(0);
		EventType eventType = event.getType();
		if (eventType == EventType.NOT_EXISTING) {
			return convertQuakemlDeleteMessage(message, event);
		} else {
			return convertQuakemlEventMessage(message, event);
		}
	}

	/**
	 * Convert a quakeml event with EventType.NOT_EXISTING to a CubeDelete.
	 * 
	 * @param message
	 *            the quakeml message
	 * @param event
	 *            the event to convert.
	 * @return CubeDelete representation of Quakeml event.
	 * @throws Exception
	 */
	public CubeDelete convertQuakemlDeleteMessage(Quakeml message, Event event)
			throws Exception {
		if (event.getType() != EventType.NOT_EXISTING) {
			// not a delete message
			return null;
		}

		// read catalog info
		if (event.getEventsource() == null || event.getEventid() == null) {
			// without an eventid, it cannot be represented as cube
			return null;
		}

		CubeDelete cubeDelete = new CubeDelete();
		cubeDelete.setSource(event.getEventsource());
		cubeDelete.setCode(event.getEventid());

		// read version and sent timestamp from event creation info
		CreationInfo creationInfo = event.getCreationInfo();
		cubeDelete.setVersion(creationInfo.getVersion());
		cubeDelete.setSent(creationInfo.getCreationTime());

		try {
			Comment deleteComment = event.getComments().get(0);
			cubeDelete.setMessage(deleteComment.getText());
		} catch (Exception e) {
			// ignore
		}

		return cubeDelete;
	}

	/**
	 * Convert a Quakeml event to a CubeEvent.
	 * 
	 * @param message
	 *            the quakeml message.
	 * @param event
	 *            the event to convert.
	 * @return CubeEvent representation of Quakeml event.
	 * @throws Exception
	 */
	public CubeEvent convertQuakemlEventMessage(Quakeml message, Event event)
			throws Exception {
		if (event.getType() == EventType.NOT_EXISTING) {
			// not a event
			return null;
		}

		// read catalog info
		if (event.getEventsource() == null || event.getEventid() == null) {
			// without an eventid, it cannot be represented as cube
			return null;
		}

		CubeEvent cubeEvent = new CubeEvent();
		cubeEvent.setSource(event.getEventsource());
		cubeEvent.setCode(event.getEventid());

		// read version and sent timestamp from event creation info
		CreationInfo creationInfo = event.getCreationInfo();
		cubeEvent.setVersion(creationInfo.getVersion());
		cubeEvent.setSent(creationInfo.getCreationTime());

		String preferredOriginID = event.getPreferredOriginID();
		String preferredMagnitudeID = event.getPreferredMagnitudeID();
		if (preferredOriginID == null) {
			// "origin" type messages include a preferred origin id, this
			// message doesn't have a cube equivalent
			return null;
		}

		Origin origin = findOrigin(event.getOrigins(), preferredOriginID);
		Magnitude magnitude = findMagnitude(event.getMagnitudes(),
				preferredMagnitudeID);

		if (origin == null || magnitude == null) {
			// the preferred information is not in this message, cannot
			// translate
			return null;
		}

		// time
		TimeQuantity time = origin.getTime();
		cubeEvent.setTime(time.getValue());

		// latitude
		RealQuantity latitude = origin.getLatitude();
		cubeEvent.setLatitude(latitude.getValue());

		// longitude
		RealQuantity longitude = origin.getLongitude();
		cubeEvent.setLongitude(longitude.getValue());

		// depth
		RealQuantity depth = origin.getDepth();
		if (depth != null) {
			cubeEvent.setDepth(depth.getValue().divide(
					CubeToQuakemlConverter.METERS_PER_KILOMETER));
		}

		// vertical uncertainty
		BigDecimal verticalError = depth.getUncertainty();
		if (verticalError != null) {
			cubeEvent.setVerticalError(depth.getUncertainty().divide(
					CubeToQuakemlConverter.METERS_PER_KILOMETER));
		}
		if (origin.getDepthType() == OriginDepthType.OPERATOR_ASSIGNED) {
			// TODO: is this necessary, operator might assign uncertainty?
			cubeEvent.setVerticalError(BigDecimal.ZERO);
		}

		// horizontal uncertainty
		OriginUncertainty originUncertainty = origin.getOriginUncertainty();
		if (originUncertainty.getPreferredDescription().equals(
				OriginUncertaintyDescription.HORIZONTAL_UNCERTAINTY)) {
			// TODO: set this regardless of preferred description?
			cubeEvent.setHorizontalError(originUncertainty
					.getHorizontalUncertainty().divide(
							CubeToQuakemlConverter.METERS_PER_KILOMETER));
		}

		OriginQuality originQuality = origin.getQuality();

		// num stations used
		BigInteger usedStations = originQuality.getUsedStationCount();
		cubeEvent.setNumLocationStations(usedStations);

		// num phases used
		BigInteger usedPhases = originQuality.getUsedPhaseCount();
		cubeEvent.setNumLocationPhases(usedPhases);

		// azimuthal gap
		cubeEvent.setAzimuthalGap(originQuality.getAzimuthalGap());

		// min station distance
		cubeEvent.setMinStationDistanceDegrees(originQuality
				.getMinimumDistance());

		// rms time error (standard error)
		cubeEvent.setRmsTimeError(originQuality.getStandardError());

		// location method
		String locationMethod = origin.getMethodID();
		cubeEvent.setLocationMethod(locationMethod);

		RealQuantity mag = magnitude.getMag();

		// magnitude type
		String magnitudeType = magnitude.getMethodID();
		if (magnitudeType != null) {
			cubeEvent.setMagnitudeType(CubeMessage.getCubeCode(MagnitudeType
					.valueOf(magnitudeType.toUpperCase())));
		}

		// magnitude
		cubeEvent.setMagnitude(mag.getValue());

		// magnitude error
		cubeEvent.setMagnitudeError(mag.getUncertainty());

		// magnitude num stations
		BigInteger numMagnitudeStations = magnitude.getStationCount();
		cubeEvent.setNumMagnitudeStations(numMagnitudeStations);

		// determine review status
		EvaluationMode originEvaluationMode = origin.getEvaluationMode();
		EvaluationMode magnitudeEvaluationMode = magnitude.getEvaluationMode();

		if (originEvaluationMode == EvaluationMode.MANUAL
				&& magnitudeEvaluationMode == EvaluationMode.MANUAL) {
			// origin AND magnitude manual
			cubeEvent.setReviewed(true);
		} else {
			// either or both are automatic
			cubeEvent.setReviewed(false);
		}

		return cubeEvent;
	}

	/**
	 * Search a list of origins for a specific origin.
	 * 
	 * @param origins
	 * @param publicID
	 * @return origin with matching public ID, or null if not found.
	 */
	public static Origin findOrigin(final List<Origin> origins,
			final String publicID) {
		Iterator<Origin> iter = origins.iterator();
		while (iter.hasNext()) {
			Origin origin = iter.next();
			if (origin.getPublicID().equals(publicID)) {
				return origin;
			}
		}
		return null;
	}

	/**
	 * Search a list of magnitudes for a specific magnitude.
	 * 
	 * @param magnitudes
	 * @param publicID
	 * @return magnitude with matching public ID, or null if not found.
	 */
	public static Magnitude findMagnitude(final List<Magnitude> magnitudes,
			final String publicID) {
		Iterator<Magnitude> iter = magnitudes.iterator();
		while (iter.hasNext()) {
			Magnitude magnitude = iter.next();
			if (magnitude.getPublicID().equals(publicID)) {
				return magnitude;
			}
		}
		return null;
	}

}