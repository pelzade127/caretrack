package com.caretrack.exception;

/** Thrown when no request matches a reference (or it belongs to another customer). Mapped to 404. */
public class RequestNotFoundException extends RuntimeException {

    private final String referenceNumber;

    public RequestNotFoundException(String referenceNumber) {
        super(buildMessage(referenceNumber));
        this.referenceNumber = referenceNumber;
    }

    /** Echo the reference back only if it looks like one, so arbitrary input isn't reflected. */
    private static String buildMessage(String referenceNumber) {
        if (referenceNumber != null && referenceNumber.matches("CT-[A-Z0-9]{1,12}")) {
            return "No support request found with reference " + referenceNumber + ".";
        }
        return "No support request found with that reference.";
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }
}
