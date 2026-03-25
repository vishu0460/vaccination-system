import React from "react";
import { Button, Modal } from "react-bootstrap";

export default function ModalPopup({
  show,
  title,
  body,
  confirmLabel = "Close",
  cancelLabel,
  onConfirm,
  onCancel,
  confirmVariant = "primary"
}) {
  return (
    <Modal show={show} onHide={onCancel || onConfirm} centered className="app-modal">
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>{body}</Modal.Body>
      <Modal.Footer>
        {cancelLabel ? (
          <Button variant="outline-secondary" onClick={onCancel}>
            {cancelLabel}
          </Button>
        ) : null}
        <Button variant={confirmVariant} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
