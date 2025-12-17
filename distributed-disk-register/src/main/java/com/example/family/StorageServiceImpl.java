package com.example.family; // Paket adın neyse ona dikkat et

import com.example.family.SetGetCommand.*;


import family.MessageId;
import family.StorageServiceGrpc;
import family.StoredMessage;
import family.StoreResult;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {

  private final DataStore dataStore;
  private static final File MESSAGE_DIR = new File("messages");

  static {
    if (!MESSAGE_DIR.exists()) {
      MESSAGE_DIR.mkdirs();
    }
  }

  // Servisimiz çalışmak için bir Veri Deposuna ihtiyaç duyar
  public StorageServiceImpl(DataStore dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  public void store(StoredMessage request, StreamObserver<StoreResult> responseObserver) {
    try {
      // 1. Gelen Protobuf mesajından verileri al
      int id = request.getId();
      String value = request.getText();

      // 2. Memory'e kaydet
      dataStore.set(id, value);

      // 3. Disk'e kaydet
      writeMessageToDisk(id, value);

      // 4. Sonucu hazırla (Başarılı)
      StoreResult result = StoreResult.newBuilder().setSuccess(true).build();

      // 5. Cevabı gönder ve işlemi kapat
      responseObserver.onNext(result);
      responseObserver.onCompleted();

      System.out.println("GRPC ile veri kaydedildi (disk+memory): " + id + " -> " + value);
    } catch (Exception e) {
      System.err.println("Store operation failed: " + e.getMessage());
      StoreResult result = StoreResult.newBuilder().setSuccess(false).build();
      responseObserver.onNext(result);
      responseObserver.onCompleted();
    }
  }

  @Override
  public void retrieve(MessageId request, StreamObserver<StoredMessage> responseObserver) {
    // 1. İstenen ID'yi al
    int id = request.getId();

    // 2. Diskten oku
    String foundValue = readMessageFromDisk(id);

    // 3. Bulunan değeri Protobuf mesajına paketle
    if (foundValue == null) {
      foundValue = "NOT_FOUND";
    }
    
    StoredMessage response = StoredMessage.newBuilder()
        .setId(request.getId())
        .setText(foundValue)
        .build();

    // 4. Cevabı gönder
    responseObserver.onNext(response);
    responseObserver.onCompleted();

    System.out.println("GRPC ile veri okundu: " + id + " -> " + foundValue);
  }

  private void writeMessageToDisk(int id, String msg) {
    File file = new File(MESSAGE_DIR, id + ".msg");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
      bw.write(msg);
    } catch (IOException e) {
      System.err.println("Failed to write message to disk: " + e.getMessage());
    }
  }

  private String readMessageFromDisk(int id) {
    File file = new File(MESSAGE_DIR, id + ".msg");
    if (!file.exists()) {
      return null;
    }

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      return br.readLine();
    } catch (IOException e) {
      System.err.println("Failed to read message from disk: " + e.getMessage());
      return null;
    }
  }
}